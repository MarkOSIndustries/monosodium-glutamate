package msg.predicates

data class Or(
  val left: Predicate,
  val right: Predicate,
) : Predicate
