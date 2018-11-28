package msg.kz

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import msg.kafka.Broker
import msg.kz.zookeeper.ZookeeperHost
import msg.kz.zookeeper.ZookeeperHosts
import org.apache.zookeeper.Watcher
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.Socket
import kotlin.streams.toList


class Show : CliktCommand(help = "Print a representation of the state of the kafka + zookeeper cluster") {
  private val zookeeper by option("--zookeeper", "-z", help = "comma separated list of zookeeper addresses",envvar = "ZOOKEEPER").default("localhost:2181")
  private val brokerPort by option("--port", "-p", help = "What port do the kafka brokers listen on?").int().default(9092) // TODO: can we get this from zk via query?

  override fun run() {
//    val inFromUser = BufferedReader(InputStreamReader(System.`in`))
//    val sentence = inFromUser.readLine()


    ZookeeperHosts.from(zookeeper).forEach{ zookeeperHost ->
//      val socket = Socket(it.host, it.port)
//      val sendStream = DataOutputStream(clientSocket.getOutputStream())
//      val zkClient = org.apache.zookeeper.ZooKeeper("${it.host}:${it.port}", 60000, Watcher { event -> /* could log watcher events*/ })
//      println("ZK State for $it - ${zkClient.state}")

      getBrokersConnectedTo(zookeeperHost).forEach { broker -> println("$broker -> $zookeeperHost") }
    }
  }


  private val clientAddressRegex:Regex = Regex("^ /(.*):\\d*\\[")
  fun getBrokersConnectedTo(zookeeperHost: ZookeeperHost): List<Broker> {
    val clientSocket = Socket(zookeeperHost.host, zookeeperHost.port)
    val outToServer = DataOutputStream(clientSocket.getOutputStream())
    val inFromServer = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
    outToServer.writeBytes("stat")
    return inFromServer.lines()
      .filter { !it.contains(":${clientSocket.localPort}") } // ignore our own connection
      .map { clientAddressRegex.find(it) }
      .filter { it != null }
      .map { it!! }
      .map { it.groupValues[1] }
      .map { Broker(it, brokerPort) }
      .toList()
  }
}
