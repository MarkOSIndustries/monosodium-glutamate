package msg.encodings.formats

import com.google.common.io.BaseEncoding
import msg.encodings.Encoding

class BinaryAsHex : Encoding.OfStringsRepresentingBinary() {
  private val encoder = BaseEncoding.base16().lowerCase()
  private val decoder = BaseEncoding.base16().ignoreCase()

  override fun encode(data: ByteArray): String = encoder.encode(data)

  override fun decode(string: String): ByteArray = decoder.decode(string)
}
