package msg.qs

import org.rocksdb.RocksIterator

class RocksDBIterator(private val rocksIterator: RocksIterator) : Iterator<Pair<ByteArray, ByteArray>> {
  override fun hasNext(): Boolean = rocksIterator.isValid

  override fun next(): Pair<ByteArray, ByteArray> {
    val pair = rocksIterator.key() to rocksIterator.value()
    rocksIterator.next()
    return pair
  }
}
