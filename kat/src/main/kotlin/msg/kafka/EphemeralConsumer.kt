package msg.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.consumer.OffsetCommitCallback
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.requests.IsolationLevel
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.time.Duration
import java.util.Locale
import java.util.UUID

class EphemeralConsumer(brokers:Collection<Broker>, vararg config:Pair<String,Any>) : KafkaConsumer<ByteArray, ByteArray>(mapOf(
  ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers.joinToString(","),
  ConsumerConfig.CLIENT_ID_CONFIG to "kat",
  ConsumerConfig.GROUP_ID_CONFIG to "kat-${UUID.randomUUID()}",
  ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
  ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
  ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
  ConsumerConfig.ISOLATION_LEVEL_CONFIG to IsolationLevel.READ_COMMITTED.toString().toLowerCase(Locale.ROOT),
  *config
)) {
  override fun commitAsync() {
    // NOOP for ephemeral consumer
  }

  override fun commitAsync(callback: OffsetCommitCallback?) {
    // NOOP for ephemeral consumer
  }

  override fun commitAsync(offsets: MutableMap<TopicPartition, OffsetAndMetadata>?, callback: OffsetCommitCallback?) {
    // NOOP for ephemeral consumer
  }

  override fun commitSync() {
    // NOOP for ephemeral consumer
  }

  override fun commitSync(offsets: MutableMap<TopicPartition, OffsetAndMetadata>?) {
    // NOOP for ephemeral consumer
  }

  override fun commitSync(timeout: Duration?) {
    // NOOP for ephemeral consumer
  }

  override fun commitSync(offsets: MutableMap<TopicPartition, OffsetAndMetadata>?, timeout: Duration?) {
    // NOOP for ephemeral consumer
  }
}
