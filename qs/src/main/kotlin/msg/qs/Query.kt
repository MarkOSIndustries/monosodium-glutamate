package msg.qs

class Query : QsCommand("Provide a GRPC query endpoint to the query store") {
  override fun run() {
    super.run()

    grpcServer.awaitTermination()
  }
}
