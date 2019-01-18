package msg.qs.encodings

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.Random

interface Encoding {
  fun reader(input: InputStream): Iterator<ByteArray>
  fun getKVPair(bytes: ByteArray): Pair<ByteArray, ByteArray>
//  fun writer(output: PrintStream): (ByteArray)->Unit

  companion object {
    private val random = Random()

    fun randomKey(): ByteArray {
      val key = ByteArray(16)
      random.nextBytes(key)
      return key
    }

    fun lengthPrefixedBinaryValues(out: OutputStream) : (ByteArray)->Unit {
      val sizeBufferArray = ByteArray(4)
      val sizeBuffer: ByteBuffer = ByteBuffer.wrap(sizeBufferArray)

      return {
        sizeBuffer.putInt(0, it.size)
        out.write(sizeBufferArray)
        out.write(it)
      }
    }
  }
}
