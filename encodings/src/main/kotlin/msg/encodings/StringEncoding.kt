package msg.encodings

sealed interface StringEncoding<T> {
  fun encode(data: T): String
  fun decode(string: String): T

  interface OfBinary : StringEncoding<ByteArray>
  interface OfStrings : StringEncoding<String>
}
