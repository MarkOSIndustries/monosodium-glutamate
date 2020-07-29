package msg.qs

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.choice
import msg.qs.RocksDBManager.Companion.countsColumnFamilyIndex
import msg.qs.RocksDBManager.Companion.defaultColumnFamilyIndex
import msg.qs.Uint64Serialisation.Companion.longToByteArray
import msg.qs.encodings.Encodings
import java.io.EOFException

class Store : QsCommand("Accept new records on stdin, while providing a GRPC query endpoint to the query store") {
  private val encoding by argument("encoding", "the stdin format for records. All are length-prefixed binary of some MSG-specific schema.").choice(Encodings.byName)

  override fun run() {
    super.run()

    try {
      val reader = encoding.reader(System.`in`)
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
