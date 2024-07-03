package msg.proto

import com.alibaba.fastjson2.JSONException
import com.alibaba.fastjson2.JSONObject
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Message
import com.google.protobuf.util.Timestamps
import msg.progressbar.NoopProgressBar
import msg.progressbar.StderrProgressBar
import msg.proto.encodings.MessageTransport
import msg.proto.protobuf.JsonParser
import msg.proto.protobuf.JsonPrinter
import msg.proto.protobuf.ProtobufMessage
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class Transform : ProtobufDataCommand() {
  private val inputEncoding by inputEncodingArgument()
  private val outputEncoding by outputEncodingArgument()
  private val inputBinaryPrefix by inputBinaryPrefixOption()
  private val outputBinaryPrefix by outputBinaryPrefixOption()

  private val limit by option("--limit", "-l", help = "the maximum number of messages to transform").long().default(Long.MAX_VALUE)
  private val filterJson by option("--filter", "-f", help = "a JSON object specifying fields and values which must match").default("{}")
  private val progress by option("--progress", help = "show a progress bar").flag()

  override fun help(context: Context) = """
  Transform protobuf messages

  Reads messages from stdin, optionally filters them, and then emits them to stdout
  """.trimIndent()

  private val jsonPrinter by lazy {
    JsonPrinter(protobufRoots.typeRegistry)
  }
  private val jsonParser by lazy {
    JsonParser(protobufRoots.typeRegistry)
  }

  override fun run() {
    val messageDescriptor = getMessageDescriptor()

    val inputCount = AtomicLong(0)
    val outputCount = AtomicLong(0)
    Runtime.getRuntime().addShutdownHook(
      Thread {
        System.err.println("Transformed $outputCount of $inputCount messages")
      }
    )

    val progressBar = if (progress) StderrProgressBar(this.commandName) else NoopProgressBar()
    if (limit != Long.MAX_VALUE) {
      progressBar.setTotal(limit)
    }

    val filterObject = try {
      JSONObject.parseObject(filterJson)
    } catch (e: JSONException) {
      System.err.println("Invalid filter JSON: ${e.message}")
      throw ProgramResult(1)
    }

    progressBar.use {
      try {
        val transport = MessageTransport(messageDescriptor)
        val reader = transport.reader(inputEncoding(protobufRoots, inputBinaryPrefix), System.`in`)
        val writer = transport.writer(outputEncoding(protobufRoots, outputBinaryPrefix), System.out)
        while (reader.hasNext() && outputCount.get() < limit) {
          val message = reader.next()
          inputCount.incrementAndGet()
          if (filter(messageDescriptor, message, filterObject)) {
            writer(message)
            progressBar.setProgress(outputCount.incrementAndGet())
          }
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
  }

  private fun filter(messageDescriptor: Descriptors.Descriptor, message: Message, filterObject: JSONObject): Boolean {
    for (key in filterObject.keys) {
      val fieldDescriptor = messageDescriptor.findFieldByName(key) ?: return false
      if (!message.hasField(fieldDescriptor)) {
        return false
      }
      val fieldValue = message.getField(fieldDescriptor)

      try {
        when (fieldDescriptor.type) {
          Descriptors.FieldDescriptor.Type.GROUP, Descriptors.FieldDescriptor.Type.MESSAGE -> {
            val protobufMessage = ProtobufMessage(protobufRoots.typeRegistry, fieldValue as Message)
            if (filterObject[key] is String && protobufMessage.messageDescriptor.fullName == "google.protobuf.Timestamp") {
              filterObject[key] = jsonPrinter.messageToJSON(Timestamps.parse(filterObject[key] as String))
            }
            if (!filter(protobufMessage.messageDescriptor, protobufMessage.message, filterObject[key] as JSONObject)) {
              return false
            }
          }

          Descriptors.FieldDescriptor.Type.BYTES -> {
            val maybeBytes = ByteString.copyFrom(filterObject.getBytes(key))
            if ((fieldValue as ByteString) != maybeBytes) {
              return false
            }
          }

          Descriptors.FieldDescriptor.Type.ENUM -> {
            if ((fieldValue as EnumValueDescriptor) != jsonParser.readField(filterObject, key, fieldDescriptor)) {
              return false
            }
          }

          else -> {
            if (fieldValue != jsonParser.readField(filterObject, key, fieldDescriptor)) {
              return false
            }
          }
        }
      } catch (e: Exception) {
        // Could be we're looking at a message that doesn't match the schema we're expecting (for instance inside an Any)
        return false
      }
    }
    return true
  }
}
