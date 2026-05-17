package msg.encodings.formats

import msg.encodings.Encoding

class Strings : Encoding.OfStringsRepresentingStrings() {
  override fun encode(data: String): String = data

  override fun decode(string: String): String = string
}
