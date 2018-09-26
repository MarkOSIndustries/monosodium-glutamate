package msg.kat.encodings

import com.google.common.io.BaseEncoding
import java.io.DataInputStream
import java.io.InputStream
import java.util.Base64

object Ingesters {
  class LengthPrefixedByteArrayIterator(input: InputStream) : Iterator<ByteArray> {
    val input = DataInputStream(input)

    override fun hasNext(): Boolean = true // TODO: how can I tie this to input still being available?

    override fun next(): ByteArray {
      val nextMessageSize = input.readInt()
      val value = ByteArray(nextMessageSize)
      input.readFully(value)
      return value
    }
  }

  fun lineDelimitedHexValues(input: InputStream): Iterator<ByteArray> {
    return input.bufferedReader().lineSequence().map { BaseEncoding.base16().lowerCase().decode(it) }.iterator()
  }

  fun lineDelimitedBase64Values(input: InputStream): Iterator<ByteArray> {
    return input.bufferedReader().lineSequence().map { Base64.getDecoder().decode(it) }.iterator()
  }

  fun lengthPrefixedBinaryValues(input: InputStream): Iterator<ByteArray> {
    return LengthPrefixedByteArrayIterator(input)
  }
}
