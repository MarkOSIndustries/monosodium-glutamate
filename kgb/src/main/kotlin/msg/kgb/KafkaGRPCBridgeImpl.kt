package msg.kgb

import msg.kafka.Broker
import msg.kafka.EphemeralConsumer
import msg.kafka.offsets.TimestampOffsetSpec
import msg.kafka.topicPartitions
import msg.schemas.MSG
import java.time.Duration

class KafkaGRPCBridgeImpl(val brokers:Collection<Broker>) : msg.schemas.KafkaGRPCBridgeGrpc.KafkaGRPCBridgeImplBase() {
  override fun consume(request: msg.schemas.MSG.ConsumeRequest,
              responseObserver: io.grpc.stub.StreamObserver<msg.schemas.MSG.KafkaRecord>) {


    responseObserver.onCompleted()
  }

  override fun offsets(request: msg.schemas.MSG.OffsetsRequest,
              responseObserver: io.grpc.stub.StreamObserver<msg.schemas.MSG.OffsetsResponse>) {
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
}
