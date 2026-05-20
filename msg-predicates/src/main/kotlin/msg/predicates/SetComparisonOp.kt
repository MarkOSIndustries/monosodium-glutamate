package msg.predicates

enum class SetComparisonOp(
  val literal: String,
) {
  AnyIn("in"),
  AllIn("[in]"),
}
