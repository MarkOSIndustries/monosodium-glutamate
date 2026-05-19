package msg.pqt

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import com.markosindustries.parquito.FileByteRangeReader
import com.markosindustries.parquito.ParquetFooter
import com.markosindustries.parquito.ParquetSchemaNode
import com.markosindustries.parquito.RowGroupReader
import com.markosindustries.parquito.RowReadSpec
import com.markosindustries.parquito.predicates.MatchAll
import com.markosindustries.parquito.predicates.ParquetPredicate
import com.markosindustries.parquito.protobuf.ProtobufReader
import msg.clikt.protobuf.outputBinaryPrefixOption
import msg.clikt.protobuf.outputEncodingArgument
import msg.pqt.clikt.ProtobufReadFileCommand
import msg.pqt.predicates.ParquitoPredicate
import msg.predicates.PredicateLanguage
import java.util.concurrent.atomic.AtomicInteger

class ReadProtobuf : ProtobufReadFileCommand() {
  private val outputEncoding by outputEncodingArgument()
  private val outputBinaryPrefix by outputBinaryPrefixOption()
  private val predicateSpec by option("--predicate", "-r", help = "a predicate specifying which records to keep").default("")
  private val limit by option("--limit", "-l", help = "the maximum number of messages to output").long().default(Long.MAX_VALUE)

  override fun help(context: Context) =
    """
    Read from a parquet file as protobuf
    """.trimIndent()

  override fun run() {
    val messageDescriptor = getMessageDescriptor()
    val outputCount = AtomicInteger(0)

    val writer = outputEncoding(messageDescriptor, protobufRoots, outputBinaryPrefix).getTransport().writer(System.out)

    val predicateBuilder: (RowGroupReader, ParquetSchemaNode.Root) -> ParquetPredicate =
      if (predicateSpec.isEmpty()) {
        { _, _ -> MatchAll() }
      } else {
        val predicateAst = PredicateLanguage.parse(predicateSpec);
        { rowGroupReader, schemaRoot -> ParquitoPredicate.build(rowGroupReader, schemaRoot, predicateAst) }
      }

    FileByteRangeReader(file).use { byteRangeReader ->
      val footer = ParquetFooter.read(byteRangeReader).join()!!
      val schema = ParquetSchemaNode.from(footer.schema)
      for (rowGroup in footer.row_groups) {
        val rowGroupReader = RowGroupReader(rowGroup, schema)
        val predicate = predicateBuilder(rowGroupReader, schema)
        val rowIterator =
          rowGroupReader.getRowIterator(
            RowReadSpec(ProtobufReader.fromDescriptor(messageDescriptor, schema), predicate),
            byteRangeReader,
          )
        while (outputCount.get() < limit && rowIterator.hasNext()) {
          val message = rowIterator.next()

          outputCount.incrementAndGet()
          writer(message)
        }
      }
    }
  }
}
