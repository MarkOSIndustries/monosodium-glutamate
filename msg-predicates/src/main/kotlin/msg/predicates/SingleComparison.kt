package msg.predicates

data class SingleComparison(
  override val dataPath: DataPath,
  val op: ComparisonOp,
  val referenceValue: String,
) : Comparison
