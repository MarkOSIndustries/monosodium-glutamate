package msg.kat

import com.github.ajalt.clikt.completion.ExperimentalCompletionCandidates
import com.github.ajalt.clikt.core.subcommands

@ExperimentalCompletionCandidates
fun main(args: Array<String>) = Kat().subcommands(Consume(), Produce(), ProduceTx(), Offsets(), Topics(), Timestamp(), Partition()).main(args)
