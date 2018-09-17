package msg.kgb

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import io.grpc.ServerBuilder
import msg.kafka.Brokers
import msg.kafka.EphemeralConsumer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.security.auth.SecurityProtocol

class Host : CliktCommand("Host the bridge on a given port") {
  val port by argument(help = "the port to bind the GRPC endpoint to").int().default(8082)
  val brokers by option("--brokers", "-b", help = "comma separated list of broker addresses",envvar = "KAFKA_BROKERS").default("localhost:9092")
  val protocol by option("--protocol", "-p", help = "the security mechanism to use to connect\nAny SASL-based protocol will require a SASL mechanism", envvar="KAFKA_PROTOCOL")
    .choice(SecurityProtocol.names().map { it to SecurityProtocol.forName(it) }.toMap()).default(SecurityProtocol.PLAINTEXT)
  val sasl by option("--sasl", "-a", help = "SASL mechanism to use for authentication. eg: SCRAM-SHA=256").default("")

  fun newConsumer(): Consumer<ByteArray, ByteArray> =
    EphemeralConsumer(
      Brokers.from(brokers),
      CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to protocol,
      "sasl.mechanism" to sasl
    )

  override fun run() {
    val server = ServerBuilder.forPort(port).addService(KafkaGRPCBridgeImpl(this::newConsumer)).build()
    server.start()

    println("Listening for GRPC requests on $port")

    // TODO: shutdown hooks etc
    server.awaitTermination()
  }
}

