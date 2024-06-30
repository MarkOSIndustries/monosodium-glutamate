package msg.proto.terminal

import me.tongfei.progressbar.ProgressBarStyle

class StderrProgressBar(name: String) : ProgressBar {
  val progressBar = me.tongfei.progressbar.ProgressBar.builder()
    .setUpdateIntervalMillis(100)
    .showSpeed()
    .hideEta()
    .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BAR)
    .setTaskName(name)
    .build()

  override fun setTotal(total: Long) {
    progressBar.maxHint(total)
  }

  override fun setProgress(progress: Long) {
    progressBar.stepTo(progress)
  }

  override fun close() {
    progressBar.close()
  }
}
