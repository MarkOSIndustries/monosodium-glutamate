package msg.qs.encodings

object Encodings {
  val byName = mapOf(
    "msg.KafkaRecord" to KafkaRecord(),
    "msg.TypedKafkaRecord" to TypedKafkaRecord(),
    "msg.PutRequest" to PutRequest()
  )
}
