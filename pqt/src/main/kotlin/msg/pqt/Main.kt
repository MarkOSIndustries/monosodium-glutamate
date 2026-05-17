package msg.pqt

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) =
  Pqt(args).subcommands(DescribeSchema(), DescribeRowGroups(), ReadJson(), ReadProtobuf(), WriteProtobuf(), AOT()).main(args)
