package msg.pqt

object HumanReadable {
  private val byteSizeSuffixes = arrayOf("B", "KB", "MB", "GB", "TB")

  fun byteCount(bytes: Long): String {
    val i = if (bytes == 0L) 0 else Math.floor(Math.log(bytes.toDouble()) / Math.log(1024.0))
    return "${"%.2f".format((bytes / Math.pow(1024.0, i.toDouble())))} ${byteSizeSuffixes[i.toInt()]}"
  }

  fun byteCount(bytes: Int): String = byteCount(bytes.toLong())
}
