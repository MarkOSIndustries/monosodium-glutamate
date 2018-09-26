package msg.kat.encodings

import java.io.OutputStream
import java.io.PrintStream
import java.nio.ByteBuffer
import java.util.Base64

object Emitters {
  fun lineDelimitedHexValues(out: PrintStream) : (ByteArray)->Unit {
    return { out.println(it.joinToString { byte -> String.format("%02X", byte) }) }
  }

  fun lineDelimitedBase64Values(out: PrintStream) : (ByteArray)->Unit {
    val encoder = Base64.getEncoder()
    return { out.println(encoder.encodeToString(it)) }
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
