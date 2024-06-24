package msg.qs

import com.github.ajalt.clikt.core.Context

class Query : QsCommand() {
  override fun help(context: Context) = "Provide a GRPC query endpoint to the query store"
  override fun run() {
    super.run()

    grpcServer.awaitTermination()
  }
}
