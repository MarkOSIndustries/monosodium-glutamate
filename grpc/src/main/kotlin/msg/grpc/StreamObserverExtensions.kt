package msg.grpc

import io.grpc.stub.ServerCallStreamObserver

fun <T, U> ServerCallStreamObserver<U>.sendWithBackpressure(iterator: Iterator<T>, vararg closeables: AutoCloseable, transformer: (T) -> U) {
  val drain = Runnable {
    while (iterator.hasNext() && isReady && !isCancelled) {
      onNext(transformer(iterator.next()))
    }
    if (!iterator.hasNext()) {
      onCompleted()
      closeables.forEach { it.close() }
    }
  }
  setOnReadyHandler(drain)
  drain.run()
}
