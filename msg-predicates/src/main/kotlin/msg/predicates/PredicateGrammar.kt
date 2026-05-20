package msg.predicates

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import msg.predicates.tokenisers.delimitedToken

object PredicateGrammar : Grammar<Predicate>() {
  // Lexer tokens
  val not by literalToken("not")
  val and by literalToken("and")
  val or by literalToken("or")
  val anyeq by literalToken(ComparisonOp.AnyEquals.literal)
  val alleq by literalToken(ComparisonOp.AllEquals.literal)
  val anyneq by literalToken(ComparisonOp.AnyNotEquals.literal)
  val allneq by literalToken(ComparisonOp.AllNotEquals.literal)
  val gte by literalToken(ComparisonOp.GreaterThanOrEqual.literal)
  val lte by literalToken(ComparisonOp.LessThanOrEqual.literal)
  val gt by literalToken(ComparisonOp.GreaterThan.literal)
  val lt by literalToken(ComparisonOp.LessThan.literal)
  val anyinset by literalToken(SetComparisonOp.AnyIn.literal)
  val allinset by literalToken(SetComparisonOp.AllIn.literal)
  val pathElement by regexToken("[a-zA-Z_][a-zA-Z0-9_]*")
  val dot by literalToken(".")
  val comma by literalToken(",")

  val openParen by literalToken("(")
  val closeParen by literalToken(")")

  val openSet by literalToken("[")
  val closeSet by literalToken("]")

  val stringLiteral by delimitedToken('"')
  val numericLiteral by regexToken("[0-9]*[.]?[0-9]+")

  val ws by regexToken("\\s+", ignore = true)

  // Parser Rules
  val dataPath by separatedTerms(pathElement, dot).map { path -> DataPath(path.map { it.text }) }
  val operator by (anyeq or alleq or anyneq or allneq or gte or lte or gt or lt).map { op ->
    ComparisonOp.entries.find { it.literal.equals(op.text) }!!
  }
  val referenceValue by (stringLiteral or numericLiteral).map {
    when (it.type) {
      stringLiteral -> it.text.substring(1, it.text.length - 1)
      numericLiteral -> it.text
      else -> it.text
    }
  }

  val singleValueSingleComparison by
    (dataPath and operator and referenceValue).map { (path, op, refValue) ->
      SingleComparison(
        path,
        op,
        refValue,
      )
    }

  val setOperator =
    (anyinset or allinset).map { op ->
      SetComparisonOp.entries.find { it.literal.equals(op.text) }!!
    }

  val setValueComparison by (
    dataPath and setOperator and skip(openSet) and separatedTerms(referenceValue, comma) and skip(closeSet)
  ).map { (path, op, refValues) ->
    SetComparison(path, op, refValues.toSet())
  }

  val term: Parser<Predicate> by
    singleValueSingleComparison or
      setValueComparison or
      (skip(not) and parser(this::term) map { Not(it) }) or
      (skip(openParen) and parser(this::rootParser) and skip(closeParen))

  val andChain by leftAssociative(term, and) { left, _, right -> And(left, right) }
  val orChain by leftAssociative(andChain or term, or) { left, _, right -> Or(left, right) }

  override val rootParser by orChain
}
