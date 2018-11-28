package msg.kafka

object Brokers {
  fun from(csvBrokers: String):List<Broker> =
    csvBrokers
      .split(",")
      .map { broker ->
        val hostAndPort = broker.split(":").map { it.trim() }
        if(hostAndPort.size == 1) {
          Broker(hostAndPort[0], 9092)
        } else {
          Broker(hostAndPort[0], hostAndPort[1].toInt())
        }
      }
      .flatMap { broker ->
        val addresses = java.net.InetAddress.getAllByName(broker.host)
        if(addresses.isEmpty()) {
          listOf(broker)
        } else {
          addresses.map { address -> Broker(address.hostAddress, broker.port) }
        }
      }
}
