package msg.predicates

data class Comparison(
  val dataPath: DataPath,
  val op: ComparisonOp,
  val comparator: String,
) : Predicate
