package msg.encodings.delimiters

import msg.encodings.StringEncoding
import msg.encodings.Transport
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream

class StringNewlineDelimiter<T>(private val stringEncoding: StringEncoding<T>) : Transport<T> {
  override fun reader(input: InputStream): Iterator<T> {
    return input.bufferedReader().lineSequence().map { stringEncoding.decode(it) }.iterator()
  }

  override fun writer(output: PrintStream): (T) -> Unit {
    return {
      output.println(stringEncoding.encode(it))
      if (output.checkError()) {
        throw IOException()
      }
    }
  }
}
