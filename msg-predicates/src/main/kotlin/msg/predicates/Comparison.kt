package msg.predicates

sealed interface Comparison : Predicate {
  val dataPath: DataPath
}
