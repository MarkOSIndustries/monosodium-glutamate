package msg.kz

import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Kgb().subcommands(Show()).main(args)
