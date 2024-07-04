package msg.kat.kafka

import java.util.Random

object RecordKeys {
  private val random = Random()

  fun randomKey(): ByteArray {
    val key = ByteArray(16)
    random.nextBytes(key)
    return key
  }
}
