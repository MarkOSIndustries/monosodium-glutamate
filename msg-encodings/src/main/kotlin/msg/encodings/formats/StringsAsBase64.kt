package msg.encodings.formats

import msg.encodings.Encoding
import java.util.Base64

class StringsAsBase64 : Encoding.OfStringsRepresentingStrings() {
  private val encoder = Base64.getEncoder()
  private val decoder = Base64.getDecoder()

  override fun encode(data: String): String = encoder.encodeToString(data.encodeToByteArray())

  override fun decode(string: String): String = decoder.decode(string).decodeToString()
}
