package msg.pqt

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.colormath.model.Ansi16
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.table.horizontalLayout
import com.markosindustries.parquito.ByteRangeReader
import com.markosindustries.parquito.ColumnChunkReader
import com.markosindustries.parquito.FileByteRangeReader
import com.markosindustries.parquito.ParquetFooter
import com.markosindustries.parquito.ParquetSchemaNode
import com.markosindustries.parquito.ParquetSchemaPath
import msg.pqt.clikt.ReadFileCommand
import org.apache.parquet.format.FileMetaData

class DescribeSchema : ReadFileCommand() {
  override fun help(context: Context) =
    """
    Describe a parquet file's schema without reading the data
    """.trimIndent()

  data class SchemaChunkSummary(
    val compressedSize: Long,
    val uncompressedSize: Long,
    val bloomCount: Int,
    val bloomSize: Long,
  )

  data object SchemaChunkSummaries {
    fun from(
      footer: FileMetaData,
      byteRangeReader: ByteRangeReader,
    ): Map<ParquetSchemaPath, SchemaChunkSummary> {
      val schema = ParquetSchemaNode.from(footer.schema)

      val chunksBySchemaPath = footer.row_groups.flatMap { it.columns }.groupBy { schema.parsePathElements(it.meta_data.path_in_schema) }
      return chunksBySchemaPath
        .map {
          var compressedSize = 0L
          var uncompressedSize = 0L
          var bloomCount = 0
          var bloomSize = 0L
          for (chunk in it.value) {
            compressedSize += chunk.meta_data.total_compressed_size
            uncompressedSize += chunk.meta_data.total_uncompressed_size
            if (chunk.meta_data.isSetBloom_filter_offset) {
              bloomCount++
              bloomSize +=
                ColumnChunkReader
                  .readBloomFilter(byteRangeReader, chunk.meta_data)
                  .join()
                  .header()
                  .numBytes
            }
          }
          it.key to SchemaChunkSummary(compressedSize, uncompressedSize, bloomCount, bloomSize)
        }.toMap()
    }
  }

  val heading = TextStyle(Ansi16(253), bold = true)

  override fun run() {
    FileByteRangeReader(file).use { byteRangeReader ->
      val footer = ParquetFooter.read(byteRangeReader).join()!!
      val schema = ParquetSchemaNode.from(footer.schema)

      val chunkSummariesByPath = SchemaChunkSummaries.from(footer, byteRangeReader)

      terminal.println(
        horizontalLayout {
          cell(renderTree(schema)) {
            align = TextAlign.LEFT
          }
          cell(
            renderColumnChunkSummary({
              HumanReadable.byteCount(it.compressedSize)
            }, chunkSummariesByPath, schema, heading("Compressed bytes")),
          ) {
            align = TextAlign.RIGHT
            spacing = 2
          }
          cell(
            renderColumnChunkSummary(
              { HumanReadable.byteCount(it.uncompressedSize) },
              chunkSummariesByPath,
              schema,
              heading("Uncompressed bytes"),
            ),
          ) {
            align = TextAlign.RIGHT
            spacing = 2
          }
          cell(
            renderColumnChunkSummary({
              if (it.bloomSize == 0L) "" else HumanReadable.byteCount(it.bloomSize)
            }, chunkSummariesByPath, schema, heading("Bloom bytes")),
          ) {
            align = TextAlign.RIGHT
            spacing = 2
          }
          cell(
            renderColumnChunkSummary({
              if (it.bloomCount == 0) "" else "${it.bloomCount}"
            }, chunkSummariesByPath, schema, heading("Bloom count")),
          ) {
            align = TextAlign.RIGHT
            spacing = 2
          }
          cell(
            renderColumnChunkSummary({
              "%.1f".format((100.0 * (it.compressedSize + it.bloomSize).toDouble() / byteRangeReader.totalBytesAvailable)) + "%"
            }, chunkSummariesByPath, schema, heading("Total file bytes %")),
          ) {
            align = TextAlign.RIGHT
            spacing = 2
          }
        },
      )
    }
  }

  fun renderColumnChunkSummary(
    render: (SchemaChunkSummary) -> String,
    chunksBySchemaPath: Map<ParquetSchemaPath, SchemaChunkSummary>,
    node: ParquetSchemaNode,
    blankLine: String = "",
  ): String {
    val sb = StringBuilder()
    if (chunksBySchemaPath.containsKey(node.path)) {
      sb.appendLine(render.invoke(chunksBySchemaPath.get(node.path)!!))
    } else {
      sb.appendLine(blankLine)
      for (node in node.children) {
        sb.append(renderColumnChunkSummary(render, chunksBySchemaPath, node))
      }
    }
    return sb.toString()
  }

  fun renderTree(node: ParquetSchemaNode): String {
    val sb = StringBuilder()
    sb.appendLine(heading(node.element.name))

    node.children.forEachIndexed { index, child ->
      val last = index == node.children.lastIndex
      sb.append(renderTree(child, "", last))
    }
    return sb.toString()
  }

  fun renderTree(
    node: ParquetSchemaNode,
    prefix: String,
    isLast: Boolean,
  ): StringBuilder {
    val connector = if (isLast) "└── " else "├── "
    val childPrefix = if (isLast) "    " else "│   "

    val sb = StringBuilder()
    sb.append(TextColors.gray("$prefix$connector"))

    sb.appendLine(if (node.children.isEmpty()) node.element.name else TextColors.blue(node.element.name))
    node.children.forEachIndexed { index, child ->
      val last = index == node.children.lastIndex
      sb.append(renderTree(child, prefix + childPrefix, last))
    }
    return sb
  }
}
