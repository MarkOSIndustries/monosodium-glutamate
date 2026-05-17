package msg.encodings.formats

import com.google.common.io.BaseEncoding
import msg.encodings.Encoding

class StringsAsHex : Encoding.OfStringsRepresentingStrings() {
  private val encoder = BaseEncoding.base16().lowerCase()
  private val decoder = BaseEncoding.base16().ignoreCase()

  override fun encode(data: String): String = encoder.encode(data.encodeToByteArray())

  override fun decode(string: String): String = decoder.decode(string).decodeToString()
}
