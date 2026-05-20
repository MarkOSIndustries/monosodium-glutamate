package msg.predicates

data class SetComparison(
  override val dataPath: DataPath,
  val op: SetComparisonOp,
  val referenceValues: Set<String>,
) : Comparison
