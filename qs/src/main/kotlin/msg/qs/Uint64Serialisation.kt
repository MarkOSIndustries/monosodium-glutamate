package msg.qs

import java.nio.ByteBuffer

interface Uint64Serialisation {
  companion object {
    fun longToByteArray(l: Long): ByteArray? {
      val buf: ByteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
      buf.putLong(l)
      return buf.array()
    }

    fun longFromByteArray(a: ByteArray): Long {
      val buf: ByteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
      buf.put(a)
      buf.flip()
      return buf.getLong()
    }
  }
}
