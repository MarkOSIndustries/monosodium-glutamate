package msg.proto.encodings.formats

import msg.proto.encodings.ProtobufEncoding
import java.util.Base64

class Base64 : ProtobufEncoding.OfStringsRepresentingBinary() {
  private val encoder = Base64.getEncoder()
  private val decoder = Base64.getDecoder()

  override fun encode(data: ByteArray): String = encoder.encodeToString(data)
  override fun decode(string: String): ByteArray = decoder.decode(string)
}
