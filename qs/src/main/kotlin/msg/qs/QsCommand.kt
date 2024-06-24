package msg.qs

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.grpc.Server
import io.grpc.ServerBuilder
import java.io.File

open class QsCommand : CliktCommand() {
  private val path by option(help = "the path to store the db files under").path(canBeFile = false)
  private val port by option(help = "the port to bind the GRPC endpoint to").int().default(8083)

  protected val rocksDBManager: RocksDBManager by lazy {
    val pathString = path?.toString() ?: "${System.getProperty("user.dir")}${File.separator}msg_qs"
    RocksDBManager.get(pathString)
  }

  protected val grpcServer: Server by lazy {
    ServerBuilder.forPort(port)
      .addService(QueryStoreImpl(rocksDBManager))
      .build()
  }

  override fun run() {
    Runtime.getRuntime().addShutdownHook(
      Thread {
        println("Shutting down...")
        grpcServer.shutdownNow()
        grpcServer.awaitTermination()
        RocksDBManager.closeAll()
        println("Done.")
      }
    )

    grpcServer.start()
    println("Listening for GRPC requests on $port")
  }
}
