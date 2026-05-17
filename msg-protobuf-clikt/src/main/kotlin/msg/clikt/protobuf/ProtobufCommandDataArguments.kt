package msg.clikt.protobuf

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import msg.encodings.delimiters.Delimiters
import msg.encodings.protobuf.ProtobufEncodings

fun ProtobufCommand.inputEncodingArgument() =
  argument("input", help = "the stdin format for messages. 'binary' means length-prefixed binary, all others are delimited strings")
    .choice(ProtobufEncodings.byName)

fun ProtobufCommand.outputEncodingArgument() =
  argument("output", help = "the stdout format for messages. 'binary' means length-prefixed binary, all others are delimited strings")
    .choice(ProtobufEncodings.byName)

fun CliktCommand.inputBinaryPrefixOption() =
  option("--input-prefix", help = "the prefix type to use for binary encoding")
    .choice(Delimiters.lengthPrefixedBinary)
    .default(Delimiters.lengthPrefixedBinary["varint"]!!)

fun CliktCommand.outputBinaryPrefixOption() =
  option("--output-prefix", help = "the prefix type to use for binary encoding")
    .choice(Delimiters.lengthPrefixedBinary)
    .default(Delimiters.lengthPrefixedBinary["varint"]!!)
