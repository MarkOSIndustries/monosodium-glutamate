package msg.clikt

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.sources.ValueSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class ProfileValueSource(
  private val values: Map<String, String>,
  private val getKey: (Context, Option) -> String = ValueSource.envvarKey(),
) : ValueSource {
  override fun getValues(context: Context, option: Option): List<ValueSource.Invocation> {
    return values[option.valueSourceKey ?: getKey(context, option)]
      ?.let {
        if (option is OptionWithValues<*, *, *> && option.valueSplit != null) {
          listOf(ValueSource.Invocation(option.valueSplit!!.split(it)))
        } else {
          ValueSource.Invocation.just(it)
        }
      }.orEmpty()
  }

  companion object {
    fun from(file: String, getKey: (Context, Option) -> String = ValueSource.envvarKey()): ProfileValueSource {
      val path = Path.of(file)
      val properties = Properties()
      if (Files.isRegularFile(path)) {
        try {
          Files.newInputStream(path).buffered().use { properties.load(it) }
        } catch (e: Throwable) {
          // Ahh well, just ignore that
        }
      }
      return ProfileValueSource(properties.entries.associate { it.key.toString() to it.value.toString() }, getKey)
    }
  }
}
