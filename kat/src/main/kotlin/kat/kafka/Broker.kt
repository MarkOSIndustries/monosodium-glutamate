package kat.kafka

class Broker(val host:String, val port:Int) {
  override fun toString(): String {
    return "$host:$port"
  }
}
