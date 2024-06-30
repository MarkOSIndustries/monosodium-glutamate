package msg.proto.terminal

interface ProgressBar : AutoCloseable {
  fun setTotal(total: Long)
  fun setProgress(progress: Long)
}
