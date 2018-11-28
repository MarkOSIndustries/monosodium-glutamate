package msg.kz.zookeeper

object ZookeeperHosts {
  fun from(csvBrokers: String):List<ZookeeperHost> =
    csvBrokers
      .split(",")
      .map { zookeeperHost ->
        val hostAndPort = zookeeperHost.split(":").map { it.trim() }
        if(hostAndPort.size == 1) {
          ZookeeperHost(hostAndPort[0], 2181)
        } else {
          ZookeeperHost(hostAndPort[0], hostAndPort[1].toInt())
        }
      }
      .flatMap { zookeeperHost ->
        val addresses = java.net.InetAddress.getAllByName(zookeeperHost.host)
        if(addresses.isEmpty()) {
          listOf(zookeeperHost)
        } else {
          addresses
            .map { address -> address.hostName }
            .toSet()
            .map { address -> ZookeeperHost(address, zookeeperHost.port) }
        }
      }
}
