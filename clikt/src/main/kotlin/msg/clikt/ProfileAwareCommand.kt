package msg.clikt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parsers.CommandLineParser
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.clikt.sources.ValueSource

abstract class ProfileAwareCommand(val args: Array<String>, name: String? = null) : CliktCommand(name) {
  val profileDirDefault = System.getProperty("user.home") + "/.msg"
  val profileDirOption = option("--profile-dir", help = "the directory MSG profiles live in", envvar = "MSG_PROFILE_DIR").default(profileDirDefault)
  val profileDir by profileDirOption

  val profileDefault = "default"
  val profileOption = option("--profile", help = "the name of the active MSG profile", envvar = "MSG_PROFILE").default(profileDefault)
  val profile by profileOption

  val profileValueSource = valueSource(args, this)

  init {
    context {
      valueSource = profileValueSource
    }
  }

  companion object {
    private fun valueSource(args: Array<String>, cmd: ProfileAwareCommand): ValueSource {
      val parse = CommandLineParser.parse(cmd, args.asList())

      val profileDir: String = parse.invocation.optionInvocations[cmd.profileDirOption]?.firstOrNull()?.values?.firstOrNull()
        ?: cmd.profileDirOption.envvar?.let { cmd.currentContext.readEnvvar(it) }
        ?: cmd.profileDirDefault
      val profile: String = parse.invocation.optionInvocations[cmd.profileOption]?.firstOrNull()?.values?.firstOrNull()
        ?: cmd.profileOption.envvar?.let { cmd.currentContext.readEnvvar(it) }
        ?: cmd.profileDefault

      return PropertiesValueSource.from("$profileDir/$profile.profile", false, ValueSource.envvarKey())
    }
  }
}
