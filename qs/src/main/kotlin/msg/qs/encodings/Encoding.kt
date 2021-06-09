package msg.qs.encodings

interface Encoding {
  fun getKVPair(bytes: ByteArray): Pair<ByteArray, ByteArray>
}
