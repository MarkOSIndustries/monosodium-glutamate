package msg.proto.grpc

import io.grpc.ManagedChannelBuilder

class GrpcHosts(hostAndPort: String, defaultPort: Int) {
  val managedChannel = hostAndPort.split(":").map { it.trim() }.let {
    if (it.size == 1) {
      ManagedChannelBuilder.forAddress(it[0], defaultPort)
    } else {
      ManagedChannelBuilder.forAddress(it[0], it[1].toInt())
    }
  }.enableRetry().usePlaintext().build()
}
