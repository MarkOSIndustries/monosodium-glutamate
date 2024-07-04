package msg.proto.grpc

import io.grpc.Attributes
import io.grpc.EquivalentAddressGroup
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.NameResolver
import io.grpc.NameResolverProvider
import io.grpc.NameResolverRegistry
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class GrpcHosts(hostsAndPorts: List<String>, defaultPort: Int) {
  val managedChannel: ManagedChannel = multiHostNameResolverProvider.prepareChannel(hostsAndPorts, defaultPort)
    .defaultLoadBalancingPolicy("round_robin")
    .enableRetry()
    .usePlaintext()
    .build()

  companion object {
    val multiHostNameResolverProvider = MultiHostNameResolverProvider()
    init {
      NameResolverRegistry.getDefaultRegistry().register(multiHostNameResolverProvider)
    }
  }

  class MultiHostNameResolverProvider() : NameResolverProvider() {
    private val authorities = ConcurrentHashMap<String, MultiHostNameResolver>()

    fun prepareChannel(hostsAndPorts: List<String>, defaultPort: Int): ManagedChannelBuilder<*> {
      val authority = "mh${nextAuthority.getAndIncrement()}"
      authorities[authority] = MultiHostNameResolver(authority, hostsAndPorts, defaultPort)
      return ManagedChannelBuilder.forTarget("multihost://$authority")
    }

    override fun newNameResolver(targetUri: URI, args: NameResolver.Args): NameResolver {
      return authorities[targetUri.authority]!!
    }

    override fun getDefaultScheme(): String = "multihost"
    override fun isAvailable(): Boolean = true
    override fun priority(): Int = 5

    companion object {
      val nextAuthority = AtomicInteger(0)
    }
  }

  class MultiHostNameResolver(private val authority: String, hostsAndPorts: List<String>, defaultPort: Int) : NameResolver() {
    private val addresses = hostsAndPorts.map { hostAndPort ->
      hostAndPort.split(':').map { it.trim() }
    }.map {
      when (it.size) {
        1 -> InetSocketAddress(it[0], defaultPort)
        2 -> InetSocketAddress(it[0], it[1].toInt())
        else -> throw URISyntaxException(it.joinToString(":"), "too many colons")
      }
    }.map { EquivalentAddressGroup(it) }

    override fun start(listener: Listener) {
      listener.onAddresses(addresses, Attributes.EMPTY)
    }

    override fun getServiceAuthority(): String = authority
    override fun shutdown() {}
  }
}
