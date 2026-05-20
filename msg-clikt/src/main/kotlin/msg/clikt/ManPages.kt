package msg.clikt

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.output.HelpFormatter
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import java.io.FileWriter
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

object ManPages {
  private val version = System.getenv("MSG_VERSION") ?: "unknown"

  fun <T : BaseCliktCommand<*>> T.manPageGenerationOption(): T =
    apply {
      registerOption(
        option(
          "--generate-man-pages-in",
          help = "",
          hidden = true,
          eager = true,
        ).path(mustExist = true, canBeDir = true, canBeFile = false).validate {
          writeManPages(context.command, it)
          throw PrintMessage("Man pages generated in $it")
        },
      )
    }

  fun writeManPages(
    command: BaseCliktCommand<*>,
    path: Path,
  ) {
    val commandPath = commandPath(command)
    FileWriter(Path(path.absolutePathString(), "${commandPath.joinToString("_")}.1").toFile()).use {
      it.write(generateManPage(commandPath.joinToString(" "), command))
      it.flush()
    }
    for (subcommand in command.registeredSubcommands()) {
      writeManPages(subcommand, path)
    }
  }

  fun generateManPage(
    commandFullName: String,
    command: BaseCliktCommand<*>,
  ): String {
    val commandName = command.commandName
    val help = command.help(command.currentContext)
    val allHelpParams = command.allHelpParams()
    val options = allHelpParams.filter { it is HelpFormatter.ParameterHelp.Option }.map { it as HelpFormatter.ParameterHelp.Option }
    val arguments = allHelpParams.filter { it is HelpFormatter.ParameterHelp.Argument }.map { it as HelpFormatter.ParameterHelp.Argument }
    val subcommands =
      allHelpParams
        .filter {
          it is HelpFormatter.ParameterHelp.Subcommand
        }.map { it as HelpFormatter.ParameterHelp.Subcommand }

    return buildString {
      appendLine(
        """
        ./" Man page for $commandName
        .TH $commandName 1 "${Instant.now().toString().split("T").first()}" "v$version" "$commandFullName -- man page"
        .SH NAME
        """.trimIndent(),
      )
      appendLine("$commandName - $help")
      appendLine(".SH SYNOPSIS")
      appendLine(
        "\\fB$commandFullName \\fI[options...] ${subcommands.joinToString(
          "\\fR|\\fI",
        ) { it.name }} ${arguments.joinToString(" ") { it.name }}\\fR",
      )
      appendLine(".SH DESCRIPTION")
      appendLine(help)
      if (subcommands.isNotEmpty()) {
        appendLine()
        appendLine(".SH SUBCOMMANDS")
        for (subcommand in subcommands) {
          appendLine(
            """
            .TP
            \fB${subcommand.name.replace("-", "\\-")}\fR
            ${subcommand.help}
            """.trimIndent(),
          )
        }
      }
      if (options.isNotEmpty()) {
        appendLine()
        appendLine(".SH OPTIONS")
        for (option in options) {
          appendLine(
            """
            .TP
            ${option.names.joinToString(", ") { """\fB${it.replace("-", "\\-")}\fR""" }}
            ${option.help}
            """.trimIndent(),
          )
        }
      }
      if (arguments.isNotEmpty()) {
        appendLine()
        appendLine(".SH ARGUMENTS")
        for (argument in arguments) {
          appendLine(
            """
            .TP
            \fB${argument.name.replace("-", "\\-")}\fR
            ${argument.help}
            """.trimIndent(),
          )
        }
      }
      appendLine(
        """
        .SH AUTHOR
        MarkOSIndustries
        .SH REPORTING BUGS
        https://www.github.com/MarkOSIndustries/monosodium-glutamate
        """.trimIndent(),
      )
    }
  }

  private fun commandPath(command: BaseCliktCommand<*>): List<String> =
    generateSequence(command.currentContext) { it.parent }
      .map { it.command.commandName }
      .toList()
      .asReversed()
}
