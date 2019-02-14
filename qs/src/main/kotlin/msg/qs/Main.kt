package msg.qs

import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Qs().subcommands(Query(), Store(), Dump()).main(args)
