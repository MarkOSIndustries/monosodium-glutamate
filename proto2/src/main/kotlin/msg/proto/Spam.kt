package msg.proto

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import msg.proto.encodings.MessageTransport
import java.io.IOException
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class Spam : ProtobufDataCommand() {
  override fun help(context: Context) = """
  Generate pseudo-random protobuf records

  Fills the specified schema with pseudo-random data, and emits it to stdout
  """.trimIndent()

  private val outputEncoding by outputEncodingArgument()
  private val outputBinaryPrefix by outputBinaryPrefixOption()

  private val limit by option("--limit", "-l", help = "the maximum number of messages to output").long().default(Long.MAX_VALUE)
  private val recursionLimit by option("--recursion-limit", "-r", help = "the maximum times a message type can be contained within itself").int().default(3)

  override fun run() {
    val messageDescriptor = getMessageDescriptor()

    val outputCount = AtomicInteger(0)
    Runtime.getRuntime().addShutdownHook(
      Thread {
        System.err.println("Spammed $outputCount messages")
      }
    )

    try {
      val transport = MessageTransport(messageDescriptor)
      val writer = transport.writer(outputEncoding(protobufRoots, outputBinaryPrefix), System.out)
      while (outputCount.get() < limit) {
        val message = spamMessage(messageDescriptor)

        outputCount.incrementAndGet()
        writer(message)
      }
    } catch (ex: com.google.protobuf.InvalidProtocolBufferException) {
      System.err.println("Invalid message: ${ex.message}")
      throw ProgramResult(1)
    } catch (_: IOException) {
      // Ignore, this will be either:
      // - we just terminated between hasNext and next()
      // - the output stream was closed
    }
  }

  private fun spamMessage(messageDescriptor: Descriptors.Descriptor, messageTypeOccurrences: List<String> = emptyList()): Message {
    val isValidField = { field: Descriptors.FieldDescriptor ->
      !field.options.hasDeprecated() &&
        !(
          field.type == Descriptors.FieldDescriptor.Type.MESSAGE &&
            messageTypeOccurrences.count { it == field.messageType.fullName } > recursionLimit
          )
    }

    val setField = { message: DynamicMessage.Builder, field: Descriptors.FieldDescriptor ->
      message.setField(field, spamField(field, messageTypeOccurrences + messageDescriptor.fullName))
    }

    return when (messageDescriptor.fullName) {
      "google.protobuf.Timestamp" -> Instant.now().let { Timestamp.newBuilder().setSeconds(it.epochSecond).setNanos(it.nano).build() }
      else -> {
        val message = DynamicMessage.newBuilder(messageDescriptor)
        for (realOneof in messageDescriptor.realOneofs) {
          val validFields = realOneof.fields.filter(isValidField)
          if (validFields.isNotEmpty()) {
            setField(message, validFields[validFields.indices.random()])
          }
        }

        val validFields = messageDescriptor.fields.filter { it.realContainingOneof == null && isValidField(it) }
        for (field in validFields) {
          setField(message, field)
        }
        return message.build()
      }
    }
  }

  private fun spamField(field: Descriptors.FieldDescriptor, messageTypeOccurrences: List<String>): Any {
    val fieldValue: Any = when (field.type!!) {
      Descriptors.FieldDescriptor.Type.DOUBLE -> Random.nextDouble() * 65536
      Descriptors.FieldDescriptor.Type.FLOAT -> Random.nextFloat() * 65536
      Descriptors.FieldDescriptor.Type.INT64,
      Descriptors.FieldDescriptor.Type.SINT64,
      Descriptors.FieldDescriptor.Type.FIXED64,
      Descriptors.FieldDescriptor.Type.SFIXED64,
      Descriptors.FieldDescriptor.Type.UINT64 -> Random.nextLong(0, 65536)
      Descriptors.FieldDescriptor.Type.INT32,
      Descriptors.FieldDescriptor.Type.SINT32,
      Descriptors.FieldDescriptor.Type.FIXED32,
      Descriptors.FieldDescriptor.Type.SFIXED32,
      Descriptors.FieldDescriptor.Type.UINT32 -> Random.nextInt(0, 65536)
      Descriptors.FieldDescriptor.Type.BOOL -> Random.nextBoolean()
      Descriptors.FieldDescriptor.Type.STRING -> "${field.name} ${Random.nextInt(0, 65536)}"
      Descriptors.FieldDescriptor.Type.GROUP -> TODO()
      Descriptors.FieldDescriptor.Type.MESSAGE -> spamMessage(field.messageType, messageTypeOccurrences)
      Descriptors.FieldDescriptor.Type.BYTES -> ByteString.copyFrom(Random.nextBytes(16))
      Descriptors.FieldDescriptor.Type.ENUM -> field.enumType.values[(0 until field.enumType.values.size).random()]
    }
    return when {
      field.isRepeated -> listOf(fieldValue)
      else -> fieldValue
    }
  }
}
