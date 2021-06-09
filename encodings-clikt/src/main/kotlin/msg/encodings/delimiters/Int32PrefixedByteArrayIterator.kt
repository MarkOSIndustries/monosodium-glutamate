package msg.encodings.delimiters

import java.io.DataInputStream
import java.io.InputStream

class Int32PrefixedByteArrayIterator(inputStream: InputStream) : Iterator<ByteArray> {
  private val input = DataInputStream(inputStream)
  private var hasNext = true
  private var nextMessageSize = tryGetNextMessageSize()

  private fun tryGetNextMessageSize(): Int = try {
    input.readInt()
  } catch (ex: Exception) {
    hasNext = false
    -1
  }

  override fun hasNext(): Boolean = hasNext

  override fun next(): ByteArray {
    val value = ByteArray(nextMessageSize)
    input.readFully(value)
    nextMessageSize = tryGetNextMessageSize()
    return value
  }
}
