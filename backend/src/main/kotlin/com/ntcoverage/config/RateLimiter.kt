package com.ntcoverage.config

import java.util.concurrent.ConcurrentHashMap

class SimpleRateLimiter(
    private val windowMs: Long,
    private val maxRequests: Int
) {
    private val requests = ConcurrentHashMap<String, MutableList<Long>>()

    fun allow(key: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = requests.compute(key) { _, existing ->
            val list = existing ?: mutableListOf()
            list.removeAll { now - it > windowMs }
            if (list.size < maxRequests) list.add(now)
            list
        }!!
        return timestamps.size <= maxRequests
    }

    fun cleanup() {
        val now = System.currentTimeMillis()
        val keysToRemove = requests.entries
            .filter { (_, timestamps) -> timestamps.all { now - it > windowMs } }
            .map { it.key }
        keysToRemove.forEach { requests.remove(it) }
    }
}
