package msg.pqt

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.table.table
import com.markosindustries.parquito.ColumnChunkReader
import com.markosindustries.parquito.FileByteRangeReader
import com.markosindustries.parquito.ParquetFooter
import com.markosindustries.parquito.ParquetSchemaNode
import msg.pqt.clikt.ReadFileCommand

class DescribeRowGroups : ReadFileCommand() {
  override fun help(context: Context) =
    """
    Describe a parquet file's row groups without reading the data
    """.trimIndent()

  override fun run() {
    FileByteRangeReader(file).use { byteRangeReader ->
      val footer = ParquetFooter.read(byteRangeReader).join()!!
      val schema = ParquetSchemaNode.from(footer.schema)

      terminal.println(
        table {
          header {
            row {
              cell("")
              cell("# Rows")
              cell("Bytes compressed")
              cell("Bytes uncompressed")
              cell("Column chunks")
            }
          }
          body {
            for (rowGroup in footer.row_groups) {
              row {
                cell("Row group")
                cell(rowGroup.num_rows)
                cell(rowGroup.total_compressed_size)
                cell(rowGroup.total_byte_size)
                cell(
                  table {
                    header {
                      row {
                        cell("Schema path")
                        cell("Encodings")
                        cell("Compressed")
                        cell("Uncompressed")
                        cell("Bloom filter?")
                      }
                    }
                    body {
                      for (columnChunk in rowGroup.columns) {
                        val meta = columnChunk.meta_data
                        row {
                          cell(meta.path_in_schema.joinToString("."))
                          cell(meta.encodings.joinToString(", "))
                          cell(meta.total_compressed_size)
                          cell(meta.total_uncompressed_size)
                          cell(
                            if (meta.isSetBloom_filter_offset) {
                              HumanReadable.byteCount(
                                ColumnChunkReader
                                  .readBloomFilter(byteRangeReader, meta)
                                  .join()
                                  .header()
                                  .numBytes,
                              )
                            } else {
                              "--"
                            },
                          )
                        }
                      }
                    }
                  },
                )
              }
            }
          }
        },
      )
    }
  }
}
