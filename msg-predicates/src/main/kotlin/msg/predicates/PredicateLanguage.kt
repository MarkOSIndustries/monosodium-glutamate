package msg.predicates

import com.github.h0tk3y.betterParse.grammar.parseToEnd

object PredicateLanguage {
  fun parse(predicate: String): Predicate = PredicateGrammar.parseToEnd(predicate)
}
