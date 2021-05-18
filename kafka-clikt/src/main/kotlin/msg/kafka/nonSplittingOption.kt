package msg.kafka

import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parsers.OptionWithValuesParser

fun nonSplittingOption(
  vararg names: String,
  help: String = "",
  metavar: String? = null,
  hidden: Boolean = false,
  envvar: String? = null
) = NullableOption(
  names = names.toSet(),
  metavarExplicit = metavar,
  metavarDefault = "TEXT",
  nvalues = 1,
  help = help,
  hidden = hidden,
  envvar = envvar,
  envvarSplit = Regex("^$"), // hopefully that never happens
  parser = OptionWithValuesParser,
  transformValue = { it },
  transformEach = { it.single() },
  transformAll = { it.joinToString(" ") }
)
