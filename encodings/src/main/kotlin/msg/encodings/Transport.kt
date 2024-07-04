package msg.encodings

import java.io.InputStream
import java.io.PrintStream

interface Transport<T> {
  fun reader(input: InputStream): Iterator<T>
  fun writer(output: PrintStream): (T) -> Unit
}
