package msg.encodings.delimiters

import java.io.InputStream
import java.io.PrintStream

class ASCIINewlineDelimiter(val encode: (ByteArray) -> String, val decode: (String) -> ByteArray) : Delimiter {
  override fun reader(input: InputStream): Iterator<ByteArray> {
    return input.bufferedReader().lineSequence().map { decode(it) }.iterator()
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return { output.println(encode(it)) }
  }
}
