package msg.proto.protobuf

import com.alibaba.fastjson2.JSONObject
import com.google.protobuf.BoolValue
import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.DoubleValue
import com.google.protobuf.DynamicMessage
import com.google.protobuf.FloatValue
import com.google.protobuf.Int32Value
import com.google.protobuf.Int64Value
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import com.google.protobuf.Timestamp
import com.google.protobuf.TypeRegistry
import com.google.protobuf.UInt32Value
import com.google.protobuf.UInt64Value
import com.google.protobuf.util.Timestamps
import java.util.Base64

class JsonPrinter(private val typeRegistry: TypeRegistry = TypeRegistry.getEmptyTypeRegistry()) {
  private val specialTypes = mapOf(
    BoolValue.getDescriptor().fullName to ::wrappedTypeToJSON,
    Int32Value.getDescriptor().fullName to ::wrappedTypeToJSON,
    UInt32Value.getDescriptor().fullName to ::wrappedTypeToJSON,
    Int64Value.getDescriptor().fullName to ::wrappedTypeToJSON,
    UInt64Value.getDescriptor().fullName to ::wrappedTypeToJSON,
    StringValue.getDescriptor().fullName to ::wrappedTypeToJSON,
    BytesValue.getDescriptor().fullName to ::wrappedTypeToJSON,
    FloatValue.getDescriptor().fullName to ::wrappedTypeToJSON,
    DoubleValue.getDescriptor().fullName to ::wrappedTypeToJSON,
    Timestamp.getDescriptor().fullName to ::timestampToJSON,
    com.google.protobuf.Any.getDescriptor().fullName to ::anyToJSON,
  )

  fun print(message: Message): String {
    return messageToJSON(message).toString()
  }

  fun messageToJSON(message: Message, vararg bonusFields: Pair<String, Any>): JSONObject {
    val jsonObject = JSONObject(message.descriptorForType.fields.size + bonusFields.size)
    for (bonusField in bonusFields) {
      jsonObject[bonusField.first] = bonusField.second
    }
    for (field in message.descriptorForType.fields) {
      when {
        field.isMapField -> {
          val keyType = field.messageType.findFieldByName("key")
          val valueType = field.messageType.findFieldByName("value")
          val entries = message.getField(field) as List<Message>
          val map = JSONObject(entries.size)
          for (entry in entries) {
            map[fieldToJSON(keyType, entry.getField(keyType)).toString()] = fieldToJSON(valueType, entry.getField(valueType))
          }
          jsonObject[field.name] = map
        }
        field.isRepeated -> jsonObject[field.name] = (message.getField(field) as List<*>).map { fieldToJSON(field, it!!) }
        else -> if (message.hasField(field)) {
          jsonObject[field.name] = fieldToJSON(field, message.getField(field))
        }
      }
    }
    return jsonObject
  }

  private fun fieldToJSON(field: Descriptors.FieldDescriptor, value: Any): Any {
    return when (field.type!!) {
      Type.DOUBLE -> value
      Type.FLOAT -> value
      Type.INT64 -> value
      Type.UINT64 -> value
      Type.INT32 -> value
      Type.FIXED64 -> value
      Type.FIXED32 -> value
      Type.BOOL -> if (value as Boolean) "true" else "false"
      Type.STRING -> value
      Type.GROUP -> TODO()
      Type.MESSAGE -> specialTypes[field.messageType.fullName]?.let { it(value as Message) } ?: messageToJSON(value as Message)
      Type.BYTES -> String(Base64.getEncoder().encode((value as ByteString).asReadOnlyByteBuffer()).array())
      Type.UINT32 -> value
      Type.ENUM -> (value as EnumValueDescriptor).name
      Type.SFIXED32 -> value
      Type.SFIXED64 -> value
      Type.SINT32 -> value
      Type.SINT64 -> value
    }
  }

  private fun wrappedTypeToJSON(message: Message): Any {
    val valueFieldDescriptor = message.descriptorForType.findFieldByName("value")
    return fieldToJSON(valueFieldDescriptor, message.getField(valueFieldDescriptor))
  }

  private fun timestampToJSON(message: Message): Any {
    return Timestamps.toString(
      when (message) {
        is Timestamp -> message
        else -> Timestamp.newBuilder()
          .setSeconds(message.getField(message.descriptorForType.findFieldByNumber(googleProtobufTimestamp_SecondsField)) as Long)
          .setNanos(message.getField(message.descriptorForType.findFieldByNumber(googleProtobufTimestamp_NanosField)) as Int)
          .build()
      }
    )
  }

  private fun anyToJSON(message: Message): Any {
    return when {
      message.descriptorForType.fullName == "google.protobuf.Any" -> {
        val typeUrlField = message.descriptorForType.findFieldByNumber(googleProtobufAny_TypeUrlField)
        val valueField = message.descriptorForType.findFieldByNumber(googleProtobufAny_ValueField)
        val typeUrl = message.getField(typeUrlField) as String
        val descriptor = typeRegistry.find(typeUrl.split("/").last())
        when {
          descriptor == null -> messageToJSON(message)
          message.hasField(valueField) ->
            messageToJSON(DynamicMessage.parseFrom(descriptor, message.getField(valueField) as ByteString), "@type" to "type/${descriptor.fullName}")
          else -> JSONObject()
        }
      }
      else -> messageToJSON(message, "@type" to "type/${message.descriptorForType.fullName}")
    }
  }

  companion object {
    private val googleProtobufTimestamp_SecondsField = Timestamp.getDescriptor().findFieldByName("seconds").number
    private val googleProtobufTimestamp_NanosField = Timestamp.getDescriptor().findFieldByName("nanos").number
    private val googleProtobufAny_TypeUrlField = com.google.protobuf.Any.getDescriptor().findFieldByName("type_url").number
    private val googleProtobufAny_ValueField = com.google.protobuf.Any.getDescriptor().findFieldByName("value").number
  }
}
