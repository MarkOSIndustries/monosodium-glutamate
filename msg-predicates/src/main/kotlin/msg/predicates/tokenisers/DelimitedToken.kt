package msg.predicates.tokenisers

import com.github.h0tk3y.betterParse.lexer.Token

class DelimitedToken(
  name: String?,
  public val delimiter: Char,
  ignored: Boolean = false,
) : Token(name, ignored) {
  override fun match(
    input: CharSequence,
    fromIndex: Int,
  ): Int {
    if (input.isEmpty()) {
      return 0
    }
    if (input[fromIndex] != delimiter) {
      return 0
    }
    for (i in fromIndex + 1 until input.length) {
      if (input[i] == delimiter) {
        return 1 + i - fromIndex
      }
    }
    return 0
  }

  override fun toString(): String = "${name ?: ""} ($delimiter[^$delimiter]*$delimiter)" + if (ignored) " [ignorable]" else ""
}

public fun delimitedToken(
  delimiter: Char,
  ignore: Boolean = false,
): Token = DelimitedToken(null, delimiter, ignore)
