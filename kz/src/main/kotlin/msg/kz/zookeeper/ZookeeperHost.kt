package msg.kz.zookeeper

class ZookeeperHost(val host:String, val port:Int) {
  override fun toString(): String {
    return "$host:$port"
  }
}
