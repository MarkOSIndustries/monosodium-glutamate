package msg.kat.encodings.formats

import com.google.common.io.BaseEncoding
import msg.kat.encodings.KafkaEncoding

class Hex : KafkaEncoding.OfStringsRepresentingBinary() {
  private val encoder = BaseEncoding.base16().lowerCase()

  override fun encode(data: ByteArray): String = encoder.encode(data)
  override fun decode(string: String): ByteArray = encoder.decode(string)
}
