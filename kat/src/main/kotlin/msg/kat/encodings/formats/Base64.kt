package msg.kat.encodings.formats

import msg.kat.encodings.KafkaEncoding
import java.util.Base64

class Base64 : KafkaEncoding.OfStringsRepresentingBinary() {
  private val encoder = Base64.getEncoder()
  private val decoder = Base64.getDecoder()

  override fun encode(data: ByteArray): String = encoder.encodeToString(data)
  override fun decode(string: String): ByteArray = decoder.decode(string)
}
