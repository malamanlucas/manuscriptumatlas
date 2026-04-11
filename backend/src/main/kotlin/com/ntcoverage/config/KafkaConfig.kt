package com.ntcoverage.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

object KafkaConfig {
    val bootstrapServers: String = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"

    const val TOPIC_LLM_RESULTS_READY = "llm-results-ready"
    const val CONSUMER_GROUP = "manuscriptum-llm-processor"

    fun producerProperties(): Properties = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.RETRIES_CONFIG, 3)
    }

    fun consumerProperties(): Properties = Properties().apply {
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP)
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10)
    }
}
