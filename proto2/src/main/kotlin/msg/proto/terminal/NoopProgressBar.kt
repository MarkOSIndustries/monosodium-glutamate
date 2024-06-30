package msg.proto.terminal

class NoopProgressBar : ProgressBar {
  override fun setTotal(total: Long) {}
  override fun setProgress(progress: Long) {}
  override fun close() {}
}
