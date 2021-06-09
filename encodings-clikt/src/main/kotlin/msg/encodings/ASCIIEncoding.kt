package msg.encodings

interface ASCIIEncoding {
  fun encode(bytes: ByteArray): String
  fun decode(string: String): ByteArray
}
