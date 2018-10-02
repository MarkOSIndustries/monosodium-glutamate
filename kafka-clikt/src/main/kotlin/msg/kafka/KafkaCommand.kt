package msg.kafka

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import kotlin.reflect.KClass

abstract class KafkaCommand(help:String) : CliktCommand(help) {
  private val brokers by option("--brokers", "-b", help = "comma separated list of broker addresses",envvar = "KAFKA_BROKERS").default("localhost:9092")
  private val protocol by option("--protocol", "-p", help = "the security mechanism to use to connect\nAny SASL-based protocol will require a SASL mechanism", envvar="KAFKA_PROTOCOL")
    .choice(*SecurityProtocol.names().toTypedArray()).default(SecurityProtocol.PLAINTEXT.toString())
  private val sasl by option("--sasl", "-a", help = "SASL mechanism to use for authentication. eg: SCRAM-SHA=256", envvar = "KAFKA_SASL").default("")

  fun <K,V,DK:Deserializer<K>,DV:Deserializer<V>>newConsumer(keyDeserialiser: KClass<DK>, valueDeserialiser: KClass<DV>, vararg config: Pair<String,Any>): Consumer<K,V> =
    EphemeralConsumer(
      Brokers.from(brokers),
      keyDeserialiser,
      valueDeserialiser,
      CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to protocol,
      "sasl.mechanism" to sasl,
      *config
    )

  fun <K,V,DK:Serializer<K>,DV:Serializer<V>>newProducer(keySerialiser: KClass<DK>, valueSerialiser: KClass<DV>, vararg config: Pair<String,Any>): Producer<K,V> = KafkaProducer(
    Brokers.from(brokers),
    keySerialiser,
    valueSerialiser,
    "monosodium-glutamate",
    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to protocol,
    "sasl.mechanism" to sasl,
    *config
  )
}
