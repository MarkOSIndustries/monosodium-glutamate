package msg.qs

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import msg.encodings.StringEncoding
import msg.encodings.Transport
import msg.encodings.delimiters.Delimiters
import msg.qs.RocksDBManager.Companion.countsColumnFamilyIndex
import msg.qs.RocksDBManager.Companion.defaultColumnFamilyIndex
import msg.qs.Uint64Serialisation.Companion.longToByteArray
import msg.qs.encodings.Encodings
import java.io.EOFException

class Store : QsCommand() {
  override fun help(context: Context) = "Accept new records on stdin, while providing a GRPC query endpoint to the query store"

  private val encoding by argument("encoding", "the stdin format for records. All are length-prefixed binary of some MSG-specific schema.").choice(Encodings.byName)
  private val prefix by option("--prefix", help = "the prefix type to use for length prefixed binary encodings").choice(*Delimiters.lengthPrefixedBinary.keys.toTypedArray()).default("varint")
  protected fun delimiter(): Transport<ByteArray> {
    return if (encoding is StringEncoding<*>) {
      val asciiEncoding = encoding as StringEncoding<ByteArray>
      Delimiters.makeStringNewlineDelimiter(asciiEncoding)
    } else {
      Delimiters.lengthPrefixedBinary[prefix]!!
    }
  }

  override fun run() {
    super.run()

    try {
      val reader = delimiter().reader(System.`in`)
      while (reader.hasNext()) {
        val bytes = reader.next()
        val kv = encoding.getKVPair(bytes)

        rocksDBManager.rocksdb.put(rocksDBManager.columnFamilyHandleList.get(defaultColumnFamilyIndex), kv.first, kv.second)
        rocksDBManager.rocksdb.merge(rocksDBManager.columnFamilyHandleList.get(countsColumnFamilyIndex), kv.first, longToByteArray(1L))
      }
    } catch (t: EOFException) {
      // Ignore, we just terminated between hasNext and next()
    }

    grpcServer.awaitTermination()
  }
}
