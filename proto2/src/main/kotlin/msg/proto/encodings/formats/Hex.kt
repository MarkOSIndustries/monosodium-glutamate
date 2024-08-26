package msg.proto.encodings.formats

import com.google.common.io.BaseEncoding
import msg.proto.encodings.ProtobufEncoding

class Hex : ProtobufEncoding.OfStringsRepresentingBinary() {
  private val encoder = BaseEncoding.base16().lowerCase()
  private val decoder = BaseEncoding.base16().ignoreCase()

  override fun encode(data: ByteArray): String = encoder.encode(data)
  override fun decode(string: String): ByteArray = decoder.decode(string)
}