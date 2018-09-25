package msg.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.consumer.OffsetCommitCallback
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Deserializer
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass

class EphemeralConsumer<K,V,DK: Deserializer<K>,DV: Deserializer<V>>(brokers:Collection<Broker>, keyDeserialiser: KClass<DK>, valueDeserialiser: KClass<DV>, vararg config:Pair<String,Any>) : KafkaConsumer<K,V,DK,DV>(
  brokers,
  keyDeserialiser,
  valueDeserialiser,
  "monosodium-glutamate",
  "monosodium-glutamate-${UUID.randomUUID()}",
  ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
  *config) {
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
