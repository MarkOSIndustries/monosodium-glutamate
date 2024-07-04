package msg.encodings.delimiters

import com.google.protobuf.CodedOutputStream
import msg.encodings.Transport
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream

class VarInt32PrefixDelimiter : Transport<ByteArray> {
  override fun reader(input: InputStream): Iterator<ByteArray> {
    return VarInt32PrefixedByteArrayIterator(input)
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    val codedOutput: CodedOutputStream = CodedOutputStream.newInstance(output, 4096)

    return {
      codedOutput.writeByteArrayNoTag(it)
      codedOutput.flush()
      if (output.checkError()) {
        throw IOException()
      }
    }
  }
}
