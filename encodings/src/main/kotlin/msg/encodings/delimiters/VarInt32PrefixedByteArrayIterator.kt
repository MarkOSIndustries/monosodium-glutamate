package msg.encodings.delimiters

import com.google.protobuf.CodedInputStream
import java.io.DataInputStream
import java.io.InputStream

class VarInt32PrefixedByteArrayIterator(inputStream: InputStream) : Iterator<ByteArray> {
  private class ResettableBox<T>(val getter: () -> T) {
    private var value: T? = null
    fun get(): T {
      if (value == null) {
        value = getter()
      }
      return value!!
    }
    fun reset() {
      value = null
    }
  }

  private val input = DataInputStream(inputStream)
  private var hasNext = true
  private var nextMessageSize = ResettableBox(this::tryGetNextMessageSize)

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

  override fun hasNext(): Boolean {
    nextMessageSize.get()
    return hasNext
  }

  override fun next(): ByteArray {
    val value = ByteArray(nextMessageSize.get())
    input.readFully(value)
    nextMessageSize.reset()
    return value
  }
}
