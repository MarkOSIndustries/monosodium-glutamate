package msg.encodings.delimiters

import msg.encodings.ASCIIEncoding

object Delimiters {
  val lengthPrefixedBinary = mapOf<String, Delimiter>(
    "Int32BE" to Int32BEPrefixDelimiter(),
    "varint" to VarInt32PrefixDelimiter(),
  )

  val ascii = mapOf<String, (ASCIIEncoding)->Delimiter>(
    "newline" to { ASCIINewlineDelimiter(it::encode, it::decode) }
  )
}
