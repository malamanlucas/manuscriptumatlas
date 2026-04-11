package com.ntcoverage.service

import com.ntcoverage.config.KafkaConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class KafkaProducerService {
    private val log = LoggerFactory.getLogger(KafkaProducerService::class.java)
    private var producer: KafkaProducer<String, String>? = null

    fun start() {
        try {
            producer = KafkaProducer(KafkaConfig.producerProperties())
            log.info("KAFKA_PRODUCER: started (bootstrap={})", KafkaConfig.bootstrapServers)
        } catch (e: Exception) {
            log.warn("KAFKA_PRODUCER: failed to start — Kafka features disabled. error={}", e.message)
        }
    }

    fun notifyResultsReady(phaseName: String) {
        val p = producer
        if (p == null) {
            log.warn("KAFKA_PRODUCER: not available, skipping notify for phase={}", phaseName)
            return
        }
        try {
            val record = ProducerRecord(KafkaConfig.TOPIC_LLM_RESULTS_READY, phaseName, phaseName)
            p.send(record) { metadata, exception ->
                if (exception != null) {
                    log.error("KAFKA_PRODUCER: failed to send phase={} error={}", phaseName, exception.message)
                } else {
                    log.info("KAFKA_PRODUCER: sent phase={} offset={}", phaseName, metadata.offset())
                }
            }
            p.flush()
        } catch (e: Exception) {
            log.error("KAFKA_PRODUCER: send error phase={} error={}", phaseName, e.message)
        }
    }

    fun close() {
        producer?.close()
        log.info("KAFKA_PRODUCER: closed")
    }
}
