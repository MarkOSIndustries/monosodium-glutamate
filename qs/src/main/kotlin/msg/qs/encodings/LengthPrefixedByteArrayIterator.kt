package msg.qs.encodings

import java.io.DataInputStream
import java.io.InputStream

class LengthPrefixedByteArrayIterator(input: InputStream) : Iterator<ByteArray> {
  private val input = DataInputStream(input)

  override fun hasNext(): Boolean = true // TODO: how can I tie this to input still being available?

  override fun next(): ByteArray {
    val nextMessageSize = input.readInt()
    val value = ByteArray(nextMessageSize)
    input.readFully(value)
    return value
  }
}
