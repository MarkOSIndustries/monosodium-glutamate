package msg.encodings.delimiters

import java.io.InputStream
import java.io.PrintStream

interface Delimiter {
  fun reader(input: InputStream): Iterator<ByteArray>
  fun writer(output: PrintStream): (ByteArray) -> Unit
}
