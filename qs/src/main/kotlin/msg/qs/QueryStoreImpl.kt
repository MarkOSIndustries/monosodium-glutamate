package msg.qs

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import msg.schemas.MSG
import msg.schemas.QueryStoreGrpc
import org.rocksdb.RocksDB

class QueryStoreImpl(private val rocksDB: RocksDB) : QueryStoreGrpc.QueryStoreImplBase() {
  override fun get(request: MSG.GetRequest, responseObserver: StreamObserver<MSG.GetResponse>) {
    try {
      val value = rocksDB.get(request.key.toByteArray())
      if (value != null) {
        responseObserver.onNext(MSG.GetResponse.newBuilder()
          .setKey(request.key)
          .setValue(Any.newBuilder().setTypeUrl(request.schema).setValue(ByteString.copyFrom(value)).build()).build())
        responseObserver.onCompleted()
      } else {
        responseObserver.onError(Status.NOT_FOUND.withDescription("Key does not exist").asRuntimeException())
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }

  override fun put(request: MSG.PutRequest, responseObserver: StreamObserver<MSG.PutResponse>) {
    try {
      rocksDB.put(request.key.toByteArray(), request.value.toByteArray())
      responseObserver.onNext(MSG.PutResponse.newBuilder().build())
      responseObserver.onCompleted()
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }

  override fun delete(request: MSG.DeleteRequest, responseObserver: StreamObserver<MSG.DeleteResponse>) {
    try {
      rocksDB.delete(request.key.toByteArray())
      responseObserver.onNext(MSG.DeleteResponse.newBuilder().build())
      responseObserver.onCompleted()
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }

  override fun scan(request: MSG.ScanRequest, responseObserver: StreamObserver<MSG.GetResponse>) {
    val limit = if (request.unlimited) Long.MAX_VALUE else request.limit
    val serverCallStreamObserver = responseObserver as ServerCallStreamObserver<MSG.GetResponse>
    try {
      rocksDB.newIterator().use { iterator ->
        iterator.seek(request.keyPrefix.toByteArray())
        var sent = 0L
        val drain = Runnable {
          while (iterator.isValid && sent < limit && serverCallStreamObserver.isReady && !serverCallStreamObserver.isCancelled) {
            responseObserver.onNext(MSG.GetResponse.newBuilder()
              .setKey(ByteString.copyFrom(iterator.key()))
              .setValue(Any.newBuilder().setTypeUrl(request.schema).setValue(ByteString.copyFrom(iterator.value())).build()).build())

            iterator.next()
            sent += 1
          }
          if (!iterator.isValid || sent == limit) {
            responseObserver.onCompleted()
          }
        }
        serverCallStreamObserver.setOnReadyHandler(drain)
        drain.run()
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }
}
