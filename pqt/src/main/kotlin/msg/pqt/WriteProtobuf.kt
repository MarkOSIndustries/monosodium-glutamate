package msg.pqt

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.uint
import com.google.protobuf.Message
import com.markosindustries.parquito.BloomFilterSelector
import com.markosindustries.parquito.ParquetSchemaNode
import com.markosindustries.parquito.RowGroupWriter
import com.markosindustries.parquito.SchemaTraversalSpecs
import com.markosindustries.parquito.WriteSpec
import com.markosindustries.parquito.protobuf.ProtobufParquetConfig
import com.markosindustries.parquito.protobuf.ProtobufSchemaConverter
import com.markosindustries.parquito.protobuf.ProtobufWriter
import msg.clikt.protobuf.inputBinaryPrefixOption
import msg.clikt.protobuf.inputEncodingArgument
import msg.pqt.clikt.ProtobufWriteFileCommand
import org.apache.parquet.format.CompressionCodec
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

class WriteProtobuf : ProtobufWriteFileCommand() {
  private val inputEncoding by inputEncodingArgument()
  private val outputBinaryPrefix by inputBinaryPrefixOption()
  private val limit by option("--limit", "-l", help = "the maximum number of messages to accept").long().default(Long.MAX_VALUE)
  private val compressionCodec by option("--compression", "-c", help = "the compression codec to use")
    .choice(
      mapOf(
        "none" to CompressionCodec.UNCOMPRESSED,
        "snappy" to CompressionCodec.SNAPPY,
        "gzip" to CompressionCodec.GZIP,
        "lzo" to CompressionCodec.LZO,
        "lz4" to CompressionCodec.LZ4,
      ),
    ).default(CompressionCodec.SNAPPY)
  private val bloomFilter by option(
    "--bloom-filter",
    "-B",
    help = "specify a column path and a false positive probability to create a bloom filter",
  ).associate()
  private val schemaRecursionLimit by option(
    "--recursion-limit",
    "-L",
    help = "the maximum depth to convert the schema to parquet",
  ).uint().default(10u)
  private val schemaPathExclusions by option("--schema-exclude", "-E", help = "exclude a schema path from the parquet file").multiple()

  override fun help(context: Context) =
    """
    Write protobuf records to a parquet file
    """.trimIndent()

  override fun run() {
    val messageDescriptor = getMessageDescriptor()
    val outputCount = AtomicInteger(0)

    val reader = inputEncoding(messageDescriptor, protobufRoots, outputBinaryPrefix).getTransport().reader(System.`in`)

    val outputStream = FileOutputStream(file)

    val protobufSchemaConverter =
      ProtobufSchemaConverter(ProtobufParquetConfig.newBuilder().withRecursionLimit(schemaRecursionLimit.toInt()).build())
    val fullSchema: ParquetSchemaNode.Root =
      protobufSchemaConverter.convertDescriptorToSchema(messageDescriptor)
    val writeSchema =
      fullSchema.trim(
        SchemaTraversalSpecs.excludeAll(
          *schemaPathExclusions
            .map {
              fullSchema.parseDotSeparatedPath(it)
            }.toTypedArray(),
        ),
      )
    val protobufWriter: ProtobufWriter<Message> = ProtobufWriter(messageDescriptor, writeSchema)

    RowGroupWriter<Message>(
      outputStream,
      WriteSpec
        .newBuilder()
        .withCompressionCodec(compressionCodec)
        .withBloomFilterSelector(
          BloomFilterSelector.fpp(
            bloomFilter.map { protobufWriter.schemaRoot.parseDotSeparatedPath(it.key) to it.value.toDouble() }.toMap(),
          ),
        ).build(),
      protobufWriter,
    ).use { writer ->
      writer.putMetaData("parquet.proto.class", messageName)
      while (outputCount.get() < limit && reader.hasNext()) {
        val message = reader.next()

        outputCount.incrementAndGet()
        writer.write(message)
      }
    }
  }
}
