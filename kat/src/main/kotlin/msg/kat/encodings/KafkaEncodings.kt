package msg.kat.encodings

object KafkaEncodings {
  val byName = mapOf(
    "hex" to Hex(),
    "base64" to Base64(),
    "binary" to Binary(),
    "msg.KafkaRecord" to KafkaRecord(),
    "msg.TypedKafkaRecord" to TypedKafkaRecord()
  )
}
