package msg.encodings.delimiters

import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.PrintStream

class VarInt32PrefixDelimiter : Delimiter {
  override fun reader(input: InputStream): Iterator<ByteArray> {
    return VarInt32PrefixedByteArrayIterator(input)
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    val codedOutput: CodedOutputStream = CodedOutputStream.newInstance(output, 4096)

    return {
      codedOutput.writeByteArrayNoTag(it)
      codedOutput.flush()
    }
  }
}
