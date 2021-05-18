package msg.kafka

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.ValueWithDefault
import com.github.ajalt.clikt.parsers.OptionWithValuesParser

fun nonSplittingOption(
  vararg names: String,
  help: String = "",
  metavar: String? = null,
  hidden: Boolean = false,
  envvar: String? = null,
  completionCandidates: CompletionCandidates? = null
) = NullableOption(
  names = names.toSet(),
  metavarWithDefault = ValueWithDefault(metavar, "TEXT"),
  nvalues = 1,
  help = help,
  hidden = hidden,
  envvar = envvar,
  envvarSplit = ValueWithDefault(null, Regex("^$")), // hopefully that never happens
  parser = OptionWithValuesParser,
  transformValue = { it },
  transformEach = { it.single() },
  transformAll = { it.joinToString(" ") },
  helpTags = emptyMap(),
  completionCandidatesWithDefault = ValueWithDefault(completionCandidates, CompletionCandidates.Path),
  valueSplit = null,
  transformValidator = {}
)
