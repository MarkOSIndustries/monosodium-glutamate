package msg.progressbar

interface ProgressBar : AutoCloseable {
  fun setTotal(total: Long)
  fun setProgress(progress: Long)
}
