package msg.predicates

data class Not(
  val predicate: Predicate,
) : Predicate
