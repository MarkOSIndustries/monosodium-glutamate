package msg.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.LongSerializer
import java.util.UUID

class Producer(brokers: Collection<Broker>, vararg config: Pair<String,Any>) {
  private val kafkaProducer = KafkaProducer<Long,ByteArray>(mapOf(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers.joinToString(","),
    ProducerConfig.CLIENT_ID_CONFIG to "kat",
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to LongSerializer::class.java.name,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java.name,
    *config
  ))

  fun produce(topic: String, valueBytes: ByteArray) {
    kafkaProducer.send(ProducerRecord(topic, UUID.randomUUID().mostSignificantBits, valueBytes))
  }
}
