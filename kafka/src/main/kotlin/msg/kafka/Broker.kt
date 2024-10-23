package msg.kafka

class Broker(
  val host: String,
  val port: Int,
) {
  override fun toString(): String = "$host:$port"
}
