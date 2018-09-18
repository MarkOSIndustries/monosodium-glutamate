package msg.kafka

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.security.auth.SecurityProtocol

abstract class KafkaCommand(help:String) : CliktCommand(help) {
  private val brokers by option("--brokers", "-b", help = "comma separated list of broker addresses",envvar = "KAFKA_BROKERS").default("localhost:9092")
  private val protocol by option("--protocol", "-p", help = "the security mechanism to use to connect\nAny SASL-based protocol will require a SASL mechanism", envvar="KAFKA_PROTOCOL")
    .choice(*SecurityProtocol.names().toTypedArray()).default(SecurityProtocol.PLAINTEXT.toString())
  private val sasl by option("--sasl", "-a", help = "SASL mechanism to use for authentication. eg: SCRAM-SHA=256", envvar = "KAFKA_SASL").default("")

  fun newConsumer(): Consumer<ByteArray, ByteArray> =
    EphemeralConsumer(
      Brokers.from(brokers),
      CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to protocol,
      "sasl.mechanism" to sasl
    )

  fun newProducer() : Producer = Producer(
    Brokers.from(brokers),
    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to protocol,
    "sasl.mechanism" to sasl
  )
}
