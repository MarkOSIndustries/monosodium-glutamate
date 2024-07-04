package msg.encodings.delimiters

import msg.encodings.StringEncoding
import msg.encodings.Transport

object Delimiters {
  val lengthPrefixedBinary = mapOf<String, Transport<ByteArray>>(
    "Int32BE" to Int32BEPrefixDelimiter(),
    "varint" to VarInt32PrefixDelimiter(),
  )

  fun <T> makeStringNewlineDelimiter(stringEncoding: StringEncoding<T>): Transport<T> {
    return StringNewlineDelimiter(stringEncoding)
  }
}
