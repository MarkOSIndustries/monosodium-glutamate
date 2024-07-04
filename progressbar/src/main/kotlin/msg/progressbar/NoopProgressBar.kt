package msg.progressbar

class NoopProgressBar : ProgressBar {
  override fun setTotal(total: Long) {}
  override fun setProgress(progress: Long) {}
  override fun close() {}
}
