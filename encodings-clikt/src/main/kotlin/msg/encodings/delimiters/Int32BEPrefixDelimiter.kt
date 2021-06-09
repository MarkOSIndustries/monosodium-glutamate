package msg.encodings.delimiters

import java.io.InputStream
import java.io.PrintStream
import java.nio.ByteBuffer

class Int32BEPrefixDelimiter : Delimiter {
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
    }
  }
}
