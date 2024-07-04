package msg.proto.grpc

import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import io.grpc.stub.StreamObserver
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class GrpcResponseWriter(val writer: (Message) -> Unit) : StreamObserver<DynamicMessage> {
  private val completed = CompletableFuture<Unit>()

  fun awaitStreamCompletion() {
    try {
      completed.get()
    } catch (ex: ExecutionException) {
      throw ex.cause ?: ex
    }
  }

  fun onStreamCompletion(completionHandler: (Throwable?) -> Unit) {
    completed.whenComplete { _, ex ->
      completionHandler(ex)
    }
  }

  override fun onNext(value: DynamicMessage) {
    writer(value)
  }

  override fun onError(t: Throwable) {
    completed.completeExceptionally(t)
  }

  override fun onCompleted() {
    completed.complete(null)
  }
}
