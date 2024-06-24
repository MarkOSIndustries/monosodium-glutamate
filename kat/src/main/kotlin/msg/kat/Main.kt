package msg.kat

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Kat(args).subcommands(Debug(), Consume(), Produce(), ProduceTx(), Offsets(), Topics(), Timestamp(), Partition()).main(args)
