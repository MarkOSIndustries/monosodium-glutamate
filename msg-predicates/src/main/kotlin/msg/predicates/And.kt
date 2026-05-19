package msg.predicates

data class And(
  val left: Predicate,
  val right: Predicate,
) : Predicate
