package msg.kgb

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.int
import io.grpc.ServerBuilder
import msg.kafka.KafkaCommand
import org.apache.kafka.common.serialization.ByteArrayDeserializer

class Host : KafkaCommand("Host the bridge on a given port") {
  private val port by argument(help = "the port to bind the GRPC endpoint to").int().default(8082)

  override fun run() {
    val server = ServerBuilder.forPort(port)
      .addService(KafkaGRPCBridgeImpl { newConsumer(ByteArrayDeserializer::class, ByteArrayDeserializer::class) })
      .build()

    Runtime.getRuntime().addShutdownHook(
      Thread {
        println("Shutting down...")
        server.shutdownNow()
        server.awaitTermination()
        println("Done.")
      }
    )

    server.start()
    println("Listening for GRPC requests on $port")

    server.awaitTermination()
  }
}
