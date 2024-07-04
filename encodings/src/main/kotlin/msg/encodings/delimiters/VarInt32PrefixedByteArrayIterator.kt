package msg.encodings.delimiters

import com.google.protobuf.CodedInputStream
import java.io.DataInputStream
import java.io.InputStream

class VarInt32PrefixedByteArrayIterator(inputStream: InputStream) : Iterator<ByteArray> {
  private val input = DataInputStream(inputStream)
  private var hasNext = true
  private var nextMessageSize = tryGetNextMessageSize()

  private fun tryGetNextMessageSize(): Int = try {
    val nextFirstByte = input.read()
    if (nextFirstByte == -1) {
      hasNext = false
      -1
    } else {
      CodedInputStream.readRawVarint32(nextFirstByte, input)
    }
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
