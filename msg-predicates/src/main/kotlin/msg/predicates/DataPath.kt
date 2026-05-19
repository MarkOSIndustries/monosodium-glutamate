package msg.predicates

data class DataPath(
  val pathElements: List<String>,
) {
  override fun toString(): String = pathElements.joinToString(".")
}
