package msg.qs

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import msg.qs.encodings.Encoding
import msg.schemas.MSG

// TODO: This is a hack, I think a better solution is a grpc CLI which can call the scan method
class Dump : QsCommand("Dump all records to stdout") {
  private val schema by option("--schema", "-s", help = "the schema to include with the value bytes").default("")

  override fun run() {
    val writer = Encoding.lengthPrefixedBinaryValues(System.`out`)
    val iterator = rocksDB.newIterator()
    iterator.seekToFirst()
    while (iterator.isValid) {
      writer(MSG.GetResponse.newBuilder()
        .setKey(ByteString.copyFrom(iterator.key()))
        .setValue(Any.newBuilder().setTypeUrl(schema).setValue(ByteString.copyFrom(iterator.value())).build()).build().toByteArray())

      iterator.next()
    }
  }
}
