package msg.kgb

import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Kgb().subcommands(Host()).main(args)
