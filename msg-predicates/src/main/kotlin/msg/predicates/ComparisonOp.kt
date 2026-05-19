package msg.predicates

enum class ComparisonOp(
  val literal: String,
) {
  AnyEquals("=="),
  AllEquals("[==]"),
  AnyNotEquals("!="),
  AllNotEquals("[!=]"),
  LessThan("<"),
  GreaterThan(">"),
  LessThanOrEqual("<="),
  GreaterThanOrEqual(">="),
}
