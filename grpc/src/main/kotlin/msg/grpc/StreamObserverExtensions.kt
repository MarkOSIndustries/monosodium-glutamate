package msg.grpc

import io.grpc.stub.ServerCallStreamObserver

fun <T, U> ServerCallStreamObserver<U>.sendWithBackpressure(iterator: Iterator<T>, transformer: (T) -> U) {
  val drain = Runnable {
    while (iterator.hasNext() && isReady && !isCancelled) {
      onNext(transformer(iterator.next()))
    }
    if (!iterator.hasNext()) {
      onCompleted()
    }
  }
  setOnReadyHandler(drain)
  drain.run()
}
