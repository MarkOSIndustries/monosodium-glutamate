package msg.proto

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Proto(args).subcommands(Transform(), Invoke(), InvokeStream(), Schemas(), Services()).main(args)
