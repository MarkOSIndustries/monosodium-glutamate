package msg.kgb

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import msg.grpc.LimitedIterator
import msg.grpc.sendWithBackpressure
import msg.kafka.TopicIterator
import msg.kafka.offsets.EarliestOffsetSpec
import msg.kafka.offsets.LatestOffsetSpec
import msg.kafka.offsets.OffsetSpec
import msg.kafka.offsets.TimestampOffsetSpec
import msg.kafka.topicPartitions
import msg.schemas.KafkaGRPCBridgeGrpc
import msg.schemas.MSG
import org.apache.kafka.clients.consumer.Consumer

class KafkaGRPCBridgeImpl(private val newConsumer: () -> Consumer<ByteArray, ByteArray>) : KafkaGRPCBridgeGrpc.KafkaGRPCBridgeImplBase() {
  override fun consume(
    request: MSG.ConsumeRequest,
    responseObserver: StreamObserver<MSG.TypedKafkaRecord>
  ) {
    try {
      val consumer = newConsumer()
      val topicIterator = TopicIterator(
        consumer,
        request.topic,
        getFromOffsetSpec(request),
        getUntilOffsetSpec(request)
      )
      val iterator = when (request.limitOneofCase) {
        MSG.ConsumeRequest.LimitOneofCase.LIMIT -> LimitedIterator(topicIterator, request.limit)
        else -> topicIterator
      }

      val schema = if (request.schema.isNullOrEmpty()) request.topic else request.schema
      (responseObserver as ServerCallStreamObserver<MSG.TypedKafkaRecord>).sendWithBackpressure(iterator, consumer) { record ->
        val builder = MSG.TypedKafkaRecord.newBuilder()
          .setTopic(record.topic())
          .setPartition(record.partition())
          .setOffset(record.offset())
          .setTimestamp(record.timestamp())

        if (record.key() != null) {
          builder.key = ByteString.copyFrom(record.key())
        }
        if (record.value() != null) {
          builder.value = Any.newBuilder().setValue(ByteString.copyFrom(record.value())).setTypeUrl("type/$schema").build()
        }

        builder.addAllHeaders(record.headers().map { header -> MSG.KafkaHeader.newBuilder().setValue(ByteString.copyFrom(header.value())).setKey(header.key()).build() })

        builder.build()
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }

  override fun offsets(
    request: MSG.OffsetsRequest,
    responseObserver: StreamObserver<MSG.OffsetsResponse>
  ) {
    try {
      newConsumer().use { consumer ->
        val partitions = consumer.topicPartitions(request.topic)

        TimestampOffsetSpec(request.timestamp).getOffsetsWithTimestamps(consumer, partitions).forEach {
          val builder = MSG.OffsetsResponse.newBuilder().setTopic(it.key.topic()).setPartition(it.key.partition())
          if (it.value != null) {
            builder.offset = it.value!!.offset()
            builder.timestamp = it.value!!.timestamp()
          }
          responseObserver.onNext(builder.build())
        }

        responseObserver.onCompleted()
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }

  private fun getFromOffsetSpec(request: MSG.ConsumeRequest): OffsetSpec =
    when (request.fromOneOfCase) {
      MSG.ConsumeRequest.FromOneOfCase.FROM_EARLIEST -> EarliestOffsetSpec()
      MSG.ConsumeRequest.FromOneOfCase.FROM_LATEST -> LatestOffsetSpec()
      MSG.ConsumeRequest.FromOneOfCase.FROM_TIMESTAMP -> TimestampOffsetSpec(request.fromTimestamp)
      else -> throw RuntimeException("Must specify which offsets to read from")
    }

  private fun getUntilOffsetSpec(request: MSG.ConsumeRequest): OffsetSpec =
    when (request.untilOneOfCase) {
      MSG.ConsumeRequest.UntilOneOfCase.UNTIL_LATEST -> LatestOffsetSpec()
      MSG.ConsumeRequest.UntilOneOfCase.UNTIL_TIMESTAMP -> TimestampOffsetSpec(request.untilTimestamp)
      else -> throw RuntimeException("Must specify which offsets to read from")
    }
}
