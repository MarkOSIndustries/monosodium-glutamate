package msg.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serializer
import kotlin.reflect.KClass

open class KafkaProducer<K,V,SK:Serializer<K>,SV: Serializer<V>>(brokers: Collection<Broker>, keySerializer: KClass<SK>, valueSerializer: KClass<SV>, clientId: String, vararg config: Pair<String,Any>) : KafkaProducer<K,V>(mapOf(
  ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers.joinToString(","),
  ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to keySerializer.java.name,
  ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer.java.name,
  ProducerConfig.CLIENT_ID_CONFIG to clientId,
  *config
)) {
}
