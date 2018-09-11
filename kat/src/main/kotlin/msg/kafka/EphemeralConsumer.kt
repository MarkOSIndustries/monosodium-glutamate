package msg.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.util.UUID

class EphemeralConsumer(vararg brokers:Broker) : KafkaConsumer<ByteArray, ByteArray>(mapOf(
  ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers.joinToString(","),
  ConsumerConfig.CLIENT_ID_CONFIG to "kat",
  ConsumerConfig.GROUP_ID_CONFIG to "kat-${UUID.randomUUID()}",
  ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
  ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
  ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false
))
