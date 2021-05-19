package msg.kafka.offsets

import org.apache.kafka.common.TopicPartition

object OffsetSpecs {
  val earliest = "earliest"
  val forever = "forever"
  val latest = "latest"

  val validCommonSpecs = arrayOf(latest, "<timestampMs>", "<timestampMs>[<partition>,<partition>,...]", "<partition>:<offset>,<partition>:<offset>,...")
  val validFromSpecs = arrayOf(earliest, *validCommonSpecs)
  val validUntilSpecs = arrayOf(forever, *validCommonSpecs)

  private val timestampRegex = Regex("""\d+""")
  private val timestampWithPartitionsRegex = Regex("""\d+\[\d+(,\d+)*\]""")
  private val partitionsWithOffsetsRegex = Regex("""\d+\:\d+(,\d+\:\d+)*""")

  fun parseOffsetSpec(spec: String, topic: String): OffsetSpec? {
    return when (spec) {
      "earliest" -> EarliestOffsetSpec()
      "latest" -> LatestOffsetSpec()
      "forever" -> MaxOffsetSpec()
      else -> {
        when {
          timestampWithPartitionsRegex.matches(spec) -> {
            val (timestamp, partitions) = spec.split('[', ']')
            TimestampAndPartitionsOffsetSpec(timestamp.toLong(), partitions.split(',').map { TopicPartition(topic, it.toInt()) })
          }
          partitionsWithOffsetsRegex.matches(spec) ->
            ConfiguredOffsetSpec(
              spec.split(',').map {
                val partitionAndOffset = it.split(':')
                TopicPartition(topic, partitionAndOffset[0].toInt()) to partitionAndOffset[1].toLong()
              }.toMap()
            )
          timestampRegex.matches(spec) -> TimestampOffsetSpec(spec.toLong())
          else -> null
        }
      }
    }
  }
}
