package msg.proto

import com.alibaba.fastjson2.JSONObject
import com.github.ajalt.clikt.core.Context
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import msg.encodings.delimiters.Delimiters
import msg.proto.encodings.ProtobufEncoding
import msg.proto.encodings.ProtobufEncodings
import msg.proto.protobuf.JsonParser
import msg.proto.protobuf.JsonPrinter
import msg.schemas.MSG
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class AOT : ProtobufCommand() {
  override fun help(context: Context) =
    """
    Exercise important code paths during the AOT compilation phase.
    """.trimIndent()

  override val hiddenFromHelp = true

  override fun run() {
    val someFileDescriptorSet =
      DescriptorProtos.FileDescriptorSet
        .newBuilder()
        .addAllFile(listOf(MSG.getDescriptor().toProto()))
        .build()
    val serialisedFileDescriptorSet = someFileDescriptorSet.toByteArray()
    val someParsedFileDescriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(serialisedFileDescriptorSet)
    val fileDescriptorSetFile = Files.createTempFile("proto_aot_exercise", ".bin")
    fileDescriptorSetFile.writeBytes(someParsedFileDescriptorSet.toByteArray())
    fileDescriptorSetFile.deleteIfExists()
    val jsonParser = JsonParser(protobufRoots.typeRegistry)
    val jsonPrinter = JsonPrinter(protobufRoots.typeRegistry)
    val messageDescriptor = protobufRoots.findMessageDescriptor("msg.KafkaHeader")!!
    val someMessage = DynamicMessage.newBuilder(messageDescriptor).build()
    val someJSONObject = JSONObject.parseObject("{}")
    val jsonFile = Files.createTempFile("proto_aot_exercise", ".json")
    jsonFile.writeText(someJSONObject.toString())
    jsonFile.deleteIfExists()
    val someEncoding = ProtobufEncodings.byName["binary"]!!(protobufRoots, Delimiters.lengthPrefixedBinary["varint"]!!)
    exerciseEncoding(someEncoding, messageDescriptor, someMessage)

    jsonParser.parse(jsonPrinter.print(someMessage), messageDescriptor)
  }

  private fun <T> exerciseEncoding(
    encoding: ProtobufEncoding<T>,
    messageDescriptor: Descriptors.Descriptor,
    message: Message,
  ) {
    encoding.toMessage(messageDescriptor, encoding.fromMessage(message))
  }
}
