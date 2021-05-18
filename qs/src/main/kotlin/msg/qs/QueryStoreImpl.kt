package msg.qs

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import msg.grpc.LimitedIterator
import msg.grpc.sendWithBackpressure
import msg.qs.Uint64Serialisation.Companion.longFromByteArray
import msg.qs.Uint64Serialisation.Companion.longToByteArray
import msg.schemas.MSG
import msg.schemas.QueryStoreGrpc

class QueryStoreImpl(private val rocksDBManager: RocksDBManager) : QueryStoreGrpc.QueryStoreImplBase() {
  override fun get(request: MSG.GetRequest, responseObserver: StreamObserver<MSG.GetResponse>) {
    try {
      val value = rocksDBManager.rocksdb.get(rocksDBManager.columnFamilyHandleList[RocksDBManager.defaultColumnFamilyIndex], request.key.toByteArray())
      if (value != null) {
        responseObserver.onNext(
          MSG.GetResponse.newBuilder()
            .setKey(request.key)
            .setValue(Any.newBuilder().setTypeUrl(request.schema).setValue(ByteString.copyFrom(value)).build()).build()
        )
        responseObserver.onCompleted()
      } else {
        responseObserver.onError(Status.NOT_FOUND.withDescription("Key does not exist").asRuntimeException())
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }

  override fun getKeyCounts(request: MSG.GetKeyCountsRequest, responseObserver: StreamObserver<MSG.GetKeyCountsResponse>) {
    try {
      val rocksIterator = rocksDBManager.rocksdb.newIterator(rocksDBManager.columnFamilyHandleList[RocksDBManager.countsColumnFamilyIndex])
      rocksIterator.seek(request.keyPrefix.toByteArray())
      val iterator = {
        val rocksDBIterator = RocksDBIterator(rocksIterator)
        when (request.limitOneofCase) {
          MSG.GetKeyCountsRequest.LimitOneofCase.LIMIT -> LimitedIterator(rocksDBIterator, request.limit)
          else -> rocksDBIterator
        }
      }()

      (responseObserver as ServerCallStreamObserver<MSG.GetKeyCountsResponse>).sendWithBackpressure(iterator, rocksIterator) { (key, value) ->
        MSG.GetKeyCountsResponse.newBuilder()
          .setKey(ByteString.copyFrom(key))
          .setCount(longFromByteArray(value)).build()
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }

  override fun put(request: MSG.PutRequest, responseObserver: StreamObserver<MSG.PutResponse>) {
    try {
      rocksDBManager.rocksdb.put(rocksDBManager.columnFamilyHandleList[RocksDBManager.defaultColumnFamilyIndex], request.key.toByteArray(), request.value.toByteArray())
      rocksDBManager.rocksdb.merge(rocksDBManager.columnFamilyHandleList.get(RocksDBManager.countsColumnFamilyIndex), request.key.toByteArray(), longToByteArray(1L))
      responseObserver.onNext(MSG.PutResponse.newBuilder().build())
      responseObserver.onCompleted()
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }

  override fun delete(request: MSG.DeleteRequest, responseObserver: StreamObserver<MSG.DeleteResponse>) {
    try {
      rocksDBManager.rocksdb.delete(rocksDBManager.columnFamilyHandleList[RocksDBManager.defaultColumnFamilyIndex], request.key.toByteArray())
      responseObserver.onNext(MSG.DeleteResponse.newBuilder().build())
      responseObserver.onCompleted()
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }

  override fun scan(request: MSG.ScanRequest, responseObserver: StreamObserver<MSG.GetResponse>) {
    try {
      val rocksIterator = rocksDBManager.rocksdb.newIterator(rocksDBManager.columnFamilyHandleList[RocksDBManager.defaultColumnFamilyIndex])
      rocksIterator.seek(request.keyPrefix.toByteArray())
      val iterator = {
        val rocksDBIterator = RocksDBIterator(rocksIterator)
        when (request.limitOneofCase) {
          MSG.ScanRequest.LimitOneofCase.LIMIT -> LimitedIterator(rocksDBIterator, request.limit)
          else -> rocksDBIterator
        }
      }()

      (responseObserver as ServerCallStreamObserver<MSG.GetResponse>).sendWithBackpressure(iterator, rocksIterator) { (key, value) ->
        MSG.GetResponse.newBuilder()
          .setKey(ByteString.copyFrom(key))
          .setValue(Any.newBuilder().setTypeUrl(request.schema).setValue(ByteString.copyFrom(value)).build()).build()
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      responseObserver.onError(Status.INTERNAL.withDescription("Internal error").withCause(t).asRuntimeException())
    }
  }
}
