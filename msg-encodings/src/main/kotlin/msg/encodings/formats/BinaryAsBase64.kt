package msg.encodings.formats

import msg.encodings.Encoding
import java.util.Base64

class BinaryAsBase64 : Encoding.OfStringsRepresentingBinary() {
  private val encoder = Base64.getEncoder()
  private val decoder = Base64.getDecoder()

  override fun encode(data: ByteArray): String = encoder.encodeToString(data)

  override fun decode(string: String): ByteArray = decoder.decode(string)
}
