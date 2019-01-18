package msg.qs

import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.WALRecoveryMode
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class RocksDBManager(private val path: String) {
  private val options: Options = Options()
    .setWalRecoveryMode(WALRecoveryMode.AbsoluteConsistency)
    .setCreateIfMissing(true)

  private val rocksdb: RocksDB by lazy {
    File(path).mkdirs()
    org.rocksdb.RocksDB.`open`(options, path)
  }

  companion object {
    private val dbsByPath: ConcurrentHashMap<String, RocksDBManager> = ConcurrentHashMap()

    fun get(path: String): RocksDB = dbsByPath.computeIfAbsent(path) { RocksDBManager(path) }.rocksdb
    fun closeAll() {
      dbsByPath.forEach { _, db -> db.rocksdb.close() }
      dbsByPath.clear()
    }
  }
}
