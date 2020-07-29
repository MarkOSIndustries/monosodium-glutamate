package msg.qs

import java.nio.ByteBuffer
import java.nio.ByteOrder

interface Uint64Serialisation {
  companion object {
    fun longToByteArray(l: Long): ByteArray {
      val buf: ByteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
      buf.order(ByteOrder.LITTLE_ENDIAN)
      buf.putLong(l)
      return buf.array()
    }

    fun longFromByteArray(a: ByteArray): Long {
      val buf: ByteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
      buf.order(ByteOrder.LITTLE_ENDIAN)
      buf.put(a)
      buf.flip()
      return buf.getLong()
    }
  }
}
