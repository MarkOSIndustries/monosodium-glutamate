package msg.qs

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Qs(args).subcommands(Query(), Store()).main(args)
