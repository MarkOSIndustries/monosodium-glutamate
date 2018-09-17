package msg.kat

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import msg.kafka.Brokers
import msg.kafka.EphemeralConsumer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.security.auth.SecurityProtocol
import java.util.Locale

abstract class KafkaTopicCommand(help:String) : CliktCommand(help) {
  val topic by argument(help = "the name of the topic")
  val brokers by option("--brokers", "-b", help = "comma separated list of broker addresses",envvar = "KAFKA_BROKERS").default("localhost:9092")
  val protocol by option("--protocol", "-p", help = "the security mechanism to use to connect\nAny SASL-based protocol will require a SASL mechanism", envvar="KAFKA_PROTOCOL")
    .choice(*SecurityProtocol.names().toTypedArray()).default("PLAINTEXT")
  val sasl by option("--sasl", "-a", help = "SASL mechanism to use for authentication. eg: SCRAM-SHA=256").default("")

  fun newConsumer(): Consumer<ByteArray, ByteArray> =
    EphemeralConsumer(
      Brokers.from(brokers),
      CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to protocol,
      "sasl.mechanism" to sasl
      )
}
