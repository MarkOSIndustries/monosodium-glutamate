package msg.kat

import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Kat().subcommands(Consume(), Produce(), Offsets()).main(args)
