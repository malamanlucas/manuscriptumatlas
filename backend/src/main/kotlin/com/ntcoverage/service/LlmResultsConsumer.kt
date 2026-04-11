package com.ntcoverage.service

import com.ntcoverage.config.KafkaConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class LlmResultsConsumer(
    private val processor: LlmResponseProcessor
) {
    private val log = LoggerFactory.getLogger(LlmResultsConsumer::class.java)
    private var job: Job? = null
    private var consumer: KafkaConsumer<String, String>? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            try {
                consumer = KafkaConsumer<String, String>(KafkaConfig.consumerProperties()).also {
                    it.subscribe(listOf(KafkaConfig.TOPIC_LLM_RESULTS_READY))
                }
                log.info("KAFKA_CONSUMER: started, listening on topic={}", KafkaConfig.TOPIC_LLM_RESULTS_READY)
                pollLoop()
            } catch (e: Exception) {
                log.warn("KAFKA_CONSUMER: failed to start — will rely on manual apply. error={}", e.message)
            }
        }
    }

    private suspend fun CoroutineScope.pollLoop() {
        val c = consumer ?: return
        while (isActive) {
            try {
                val records = c.poll(Duration.ofSeconds(5))
                if (records.isEmpty) continue

                for (record in records) {
                    val phaseName = record.value()
                    log.info("KAFKA_CONSUMER: received phase={} offset={}", phaseName, record.offset())
                    try {
                        val result = processor.processCompleted(phaseName)
                        log.info("KAFKA_CONSUMER: processed phase={} applied={} errors={}", phaseName, result.applied, result.errors)
                    } catch (e: Exception) {
                        log.error("KAFKA_CONSUMER: processing failed phase={} error={}", phaseName, e.message, e)
                    }
                }
                c.commitSync()
            } catch (e: org.apache.kafka.common.errors.WakeupException) {
                break
            } catch (e: Exception) {
                log.error("KAFKA_CONSUMER: poll error={}", e.message, e)
                Thread.sleep(5000)
            }
        }
    }

    fun close() {
        consumer?.wakeup()
        job?.cancel()
        consumer?.close()
        log.info("KAFKA_CONSUMER: closed")
    }
}
