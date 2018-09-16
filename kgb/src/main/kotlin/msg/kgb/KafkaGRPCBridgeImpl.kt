package msg.kgb

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import msg.kafka.Broker
import msg.kafka.Brokers
import msg.kafka.EphemeralConsumer
import msg.kafka.TopicIterator
import msg.kafka.offsets.EarliestOffsetSpec
import msg.kafka.offsets.LatestOffsetSpec
import msg.kafka.offsets.OffsetSpec
import msg.kafka.offsets.TimestampOffsetSpec
import msg.kafka.topicPartitions
import msg.schemas.KafkaGRPCBridgeGrpc
import msg.schemas.MSG
import java.lang.RuntimeException
import java.time.Duration

class KafkaGRPCBridgeImpl(private val brokers:Collection<Broker>) : KafkaGRPCBridgeGrpc.KafkaGRPCBridgeImplBase() {
  override fun consume(request: MSG.ConsumeRequest,
              responseObserver: io.grpc.stub.StreamObserver<MSG.TypedKafkaRecord>) {
    val iterator = TopicIterator(
      EphemeralConsumer(brokers),
      request.topic,
      getFromOffsetSpec(request),
      getUntilOffsetSpec(request)
    )

    val schema = if(request.schema.isNullOrEmpty()) request.topic else request.schema
    val limit = if(request.limit <= 0L) Long.MAX_VALUE else request.limit
    var count = 0
    while(iterator.hasNext() && count++ < limit) {
      val record = iterator.next()
      val builder = MSG.TypedKafkaRecord.newBuilder()
        .setTopic(record.topic())
        .setPartition(record.partition())
        .setOffset(record.offset())
        .setTimestamp(record.timestamp())

      if(record.key() != null) {
        builder.key = ByteString.copyFrom(record.key())
      }
      if(record.value() != null) {
        builder.value = Any.newBuilder().setValue(ByteString.copyFrom(record.value())).setTypeUrl(schema).build()
      }
      responseObserver.onNext(builder.build())
    }

    responseObserver.onCompleted()
  }

  override fun offsets(request: MSG.OffsetsRequest,
              responseObserver: StreamObserver<MSG.OffsetsResponse>) {
    val consumer = EphemeralConsumer(brokers)
    val partitions = consumer.topicPartitions(request.topic, Duration.ofMinutes(1))

    TimestampOffsetSpec(request.timestamp).getOffsetsWithTimestamps(consumer, partitions).forEach {
      val builder = MSG.OffsetsResponse.newBuilder().setTopic(it.key.topic()).setPartition(it.key.partition())
      if(it.value != null) {
        builder.offset = it.value!!.offset()
        builder.timestamp = it.value!!.timestamp()
      }
      responseObserver.onNext(builder.build())
    }

    responseObserver.onCompleted()
  }

  private fun getFromOffsetSpec(request: MSG.ConsumeRequest): OffsetSpec =
    when(request.fromOneOfCase) {
      MSG.ConsumeRequest.FromOneOfCase.FROM_EARLIEST -> EarliestOffsetSpec()
      MSG.ConsumeRequest.FromOneOfCase.FROM_LATEST -> LatestOffsetSpec()
      MSG.ConsumeRequest.FromOneOfCase.FROM_TIMESTAMP -> TimestampOffsetSpec(request.fromTimestamp)
      else -> throw RuntimeException("Must specify which offsets to read from")
    }

  private fun getUntilOffsetSpec(request: MSG.ConsumeRequest): OffsetSpec =
    when(request.untilOneOfCase) {
      MSG.ConsumeRequest.UntilOneOfCase.UNTIL_LATEST -> LatestOffsetSpec()
      MSG.ConsumeRequest.UntilOneOfCase.UNTIL_TIMESTAMP -> TimestampOffsetSpec(request.untilTimestamp)
      else -> throw RuntimeException("Must specify which offsets to read from")
    }
}
