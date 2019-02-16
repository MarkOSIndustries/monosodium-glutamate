package msg.grpc

class LimitedIterator<T>(private val iterator: Iterator<T>, private val limit: Long) : Iterator<T> {
  private var count = 0L

  override fun hasNext(): Boolean = iterator.hasNext() && count < limit

  override fun next(): T {
    count += 1
    return iterator.next()
  }
}
