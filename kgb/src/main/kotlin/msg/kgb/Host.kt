package msg.kgb

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.grpc.ServerBuilder
import msg.kafka.Brokers
import msg.kafka.EphemeralConsumer
import msg.kafka.offsets.TimestampOffsetSpec
import msg.kafka.topicPartitions
import java.time.Duration

class Host : CliktCommand("Host the bridge on a given port") {
  val port by argument(help = "the port to bind the GRPC endpoint to").int().default(8082)
  val brokers by option("--brokers", "-b", help = "comma separated list of broker addresses",envvar = "KAFKA_BROKERS").default("localhost:9092")

  override fun run() {
    val server = ServerBuilder.forPort(port).addService(KafkaGRPCBridgeImpl(Brokers.from(brokers))).build()
    server.start()

    // TODO: shutdown hooks etc
    server.awaitTermination()
  }
}

