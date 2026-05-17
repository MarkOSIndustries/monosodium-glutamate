package msg.encodings

import msg.encodings.delimiters.StringNewlineDelimiter

interface Encoding<T> {
  fun getTransport(): Transport<T>

  abstract class OfBinary(
    private val transport: Transport<ByteArray>,
  ) : Encoding<ByteArray> {
    final override fun getTransport(): Transport<ByteArray> = transport
  }

  abstract class OfStringsRepresentingBinary : StringEncoding.OfBinary {
    final override fun getTransport(): Transport<ByteArray> = StringNewlineDelimiter(this)
  }

  abstract class OfStringsRepresentingStrings : StringEncoding.OfStrings {
    final override fun getTransport(): Transport<String> = StringNewlineDelimiter(this)
  }
}
