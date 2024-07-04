package msg.encodings.delimiters

import msg.encodings.Transport
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import java.nio.ByteBuffer

class Int32BEPrefixDelimiter : Transport<ByteArray> {
  override fun reader(input: InputStream): Iterator<ByteArray> {
    return Int32PrefixedByteArrayIterator(input)
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    val sizeBufferArray = ByteArray(4)
    val sizeBuffer: ByteBuffer = ByteBuffer.wrap(sizeBufferArray)

    return {
      sizeBuffer.putInt(0, it.size)
      output.write(sizeBufferArray)
      output.write(it)
      if (output.checkError()) {
        throw IOException()
      }
    }
  }
}
