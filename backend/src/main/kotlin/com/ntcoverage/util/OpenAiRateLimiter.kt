package com.ntcoverage.util

import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.random.Random

/**
 * Exception for non-retryable OpenAI errors (billing, quota, account issues).
 * Services should catch this and fail immediately without retry.
 */
class OpenAiNonRetryableException(message: String) : RuntimeException(message)

/**
 * Centralized rate limiter and retry handler for OpenAI API calls.
 * Adds a delay between consecutive calls, proactive throttling via rate limit headers,
 * and retries on 429 (Too Many Requests) with exponential backoff + jitter.
 */
object OpenAiRateLimiter {
    private val log = LoggerFactory.getLogger(OpenAiRateLimiter::class.java)
    private val mutex = Mutex()
    private var lastCallTime = 0L

    /** Minimum delay in ms between consecutive OpenAI calls (default 500ms, configurable via env) */
    private val minDelayMs: Long = System.getenv("OPENAI_CALL_DELAY_MS")?.toLongOrNull() ?: 500L

    /** Max retries on 429 */
    private val maxRetries: Int = System.getenv("OPENAI_MAX_RETRIES")?.toIntOrNull() ?: 5

    /** Initial backoff wait in ms when receiving 429 */
    private val initialBackoffMs: Long = System.getenv("OPENAI_BACKOFF_MS")?.toLongOrNull() ?: 5_000L

    /** Non-retryable error codes from OpenAI that indicate account/billing issues */
    private val nonRetryableErrors = listOf(
        "billing_not_active",
        "insufficient_quota",
        "account_deactivated",
        "invalid_api_key"
    )

    /**
     * Executes an OpenAI API call with rate limiting and 429 retry.
     *
     * @param label descriptive label for logging
     * @param execute the suspend lambda that performs the HTTP call and returns the HttpResponse
     * @return the response body as String
     * @throws OpenAiNonRetryableException if the error is a billing/quota issue (should not be retried)
     * @throws RuntimeException if all retries are exhausted or a non-429 error occurs
     */
    suspend fun executeWithResilience(label: String, execute: suspend () -> HttpResponse): String {
        var attempt = 0
        var backoffMs = initialBackoffMs

        while (true) {
            // Rate limit: ensure minimum delay between calls
            mutex.withLock {
                val now = System.currentTimeMillis()
                val elapsed = now - lastCallTime
                if (elapsed < minDelayMs) {
                    delay(minDelayMs - elapsed)
                }
                lastCallTime = System.currentTimeMillis()
            }

            val response = execute()
            val body = response.bodyAsText()

            when {
                response.status == HttpStatusCode.OK -> {
                    // Proactive throttling: if remaining requests is low, wait before next call
                    val remaining = response.headers["x-ratelimit-remaining-requests"]?.toIntOrNull()
                    if (remaining != null && remaining <= 2) {
                        val resetDuration = parseResetDuration(response.headers["x-ratelimit-reset-requests"])
                        if (resetDuration > 0) {
                            log.info("OPENAI_PROACTIVE_THROTTLE: {} remaining requests={}, waiting {}ms until reset", label, remaining, resetDuration)
                            mutex.withLock {
                                lastCallTime = System.currentTimeMillis() + resetDuration
                            }
                        }
                    }
                    return body
                }

                response.status == HttpStatusCode.TooManyRequests -> {
                    // Check for non-retryable errors (billing, quota, account issues)
                    if (nonRetryableErrors.any { it in body }) {
                        log.error("OPENAI_NON_RETRYABLE: {} — account/billing error detected, aborting immediately: {}", label, body.take(500))
                        throw OpenAiNonRetryableException("OpenAI account error for $label: ${body.take(500)}")
                    }

                    attempt++
                    if (attempt > maxRetries) {
                        throw RuntimeException("OpenAI 429 after $maxRetries retries for $label: $body")
                    }

                    // Use Retry-After header if present, otherwise exponential backoff with jitter
                    val retryAfterSec = response.headers["Retry-After"]?.toLongOrNull()
                    val jitter = 1.0 + Random.nextDouble() * 0.5 // 1.0 to 1.5 multiplier
                    val waitMs = if (retryAfterSec != null) {
                        (retryAfterSec * 1000 * jitter).toLong()
                    } else {
                        (backoffMs * jitter).toLong()
                    }

                    log.warn(
                        "OPENAI_RATE_LIMITED: 429 for {} (attempt {}/{}), waiting {}ms before retry",
                        label, attempt, maxRetries, waitMs
                    )
                    delay(waitMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(60_000L) // cap at 60s
                }

                response.status.value == 401 || response.status.value == 403 -> {
                    // Auth errors are non-retryable
                    log.error("OPENAI_AUTH_ERROR: {} — status={}: {}", label, response.status, body.take(500))
                    throw OpenAiNonRetryableException("OpenAI auth error for $label (${response.status}): ${body.take(500)}")
                }

                else -> {
                    throw RuntimeException("OpenAI status=${response.status} for $label: $body")
                }
            }
        }
    }

    /**
     * Parses OpenAI reset duration format (e.g., "1s", "6m0s", "1m30s") to milliseconds.
     */
    internal fun parseResetDuration(value: String?): Long {
        if (value.isNullOrBlank()) return 0L

        var totalMs = 0L
        val minuteMatch = Regex("""(\d+)m""").find(value)
        val secondMatch = Regex("""(\d+)s""").find(value)

        if (minuteMatch != null) {
            totalMs += (minuteMatch.groupValues[1].toLongOrNull() ?: 0L) * 60_000L
        }
        if (secondMatch != null) {
            totalMs += (secondMatch.groupValues[1].toLongOrNull() ?: 0L) * 1_000L
        }

        return totalMs
    }
}
