package msg.qs

import org.rocksdb.ColumnFamilyDescriptor
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ColumnFamilyOptions
import org.rocksdb.DBOptions
import org.rocksdb.RocksDB
import org.rocksdb.UInt64AddOperator
import org.rocksdb.WALRecoveryMode
import java.io.File
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import java.util.ArrayList

class RocksDBManager(private val path: String) {
  var cfDefaultOpts = ColumnFamilyOptions()
  var cfCountOpts = ColumnFamilyOptions().setMergeOperator(UInt64AddOperator())

  val cfDescriptors = Arrays.asList(
    ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfDefaultOpts),
    ColumnFamilyDescriptor("counts".toByteArray(), cfCountOpts)
  )

  private val options: DBOptions = DBOptions()
    .setWalRecoveryMode(WALRecoveryMode.AbsoluteConsistency)
    .setCreateIfMissing(true)
    .setCreateMissingColumnFamilies(true)

  public val columnFamilyHandleList = ArrayList<ColumnFamilyHandle>()
  public val rocksdb: RocksDB by lazy {
    File(path).mkdirs()
    org.rocksdb.RocksDB.`open`(options, path, cfDescriptors, columnFamilyHandleList)
  }

  companion object {
    public val defaultColumnFamilyIndex = 0
    public val countsColumnFamilyIndex = 1
    private val dbsByPath: ConcurrentHashMap<String, RocksDBManager> = ConcurrentHashMap()

    fun get(path: String): RocksDBManager = dbsByPath.computeIfAbsent(path) { RocksDBManager(path) }
    fun closeAll() {
      dbsByPath.forEach { _, db -> db.rocksdb.close() }
      dbsByPath.clear()
    }
  }
}
