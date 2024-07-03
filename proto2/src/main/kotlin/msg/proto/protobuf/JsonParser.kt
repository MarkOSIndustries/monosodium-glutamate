package msg.proto.protobuf

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.google.protobuf.BoolValue
import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.DoubleValue
import com.google.protobuf.DynamicMessage
import com.google.protobuf.FloatValue
import com.google.protobuf.Int32Value
import com.google.protobuf.Int64Value
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import com.google.protobuf.Timestamp
import com.google.protobuf.TypeRegistry
import com.google.protobuf.UInt32Value
import com.google.protobuf.UInt64Value
import com.google.protobuf.util.Timestamps
import java.util.Base64

class JsonParser(private val typeRegistry: TypeRegistry = TypeRegistry.getEmptyTypeRegistry()) {
  private val specialTypes = mapOf(
    BoolValue.getDescriptor().fullName to ::readWrappedType,
    Int32Value.getDescriptor().fullName to ::readWrappedType,
    UInt32Value.getDescriptor().fullName to ::readWrappedType,
    Int64Value.getDescriptor().fullName to ::readWrappedType,
    UInt64Value.getDescriptor().fullName to ::readWrappedType,
    StringValue.getDescriptor().fullName to ::readWrappedType,
    BytesValue.getDescriptor().fullName to ::readWrappedType,
    FloatValue.getDescriptor().fullName to ::readWrappedType,
    DoubleValue.getDescriptor().fullName to ::readWrappedType,
    Timestamp.getDescriptor().fullName to ::readTimestamp,
    com.google.protobuf.Any.getDescriptor().fullName to ::readAny,
  )

  fun parse(data: String, messageDescriptor: Descriptors.Descriptor): Message {
    val jsonObject = JSONObject.parseObject(data)
    return readMessage(jsonObject, messageDescriptor)
  }

  private fun readWrappedType(value: Any, field: Descriptors.FieldDescriptor): Message {
    val valueFieldDescriptor = field.messageType.findFieldByName("value")
    return DynamicMessage.newBuilder(field.messageType).setField(
      valueFieldDescriptor,
      if (value is JSONObject) {
        readField(value, "value", valueFieldDescriptor)
      } else {
        when (valueFieldDescriptor.type!!) {
          Type.DOUBLE -> if (value is String) value.toDouble() else value as Double
          Type.FLOAT -> if (value is String) value.toFloat() else value as Float
          Type.INT64,
          Type.SINT64,
          Type.FIXED64,
          Type.SFIXED64,
          Type.UINT64 -> if (value is String) value.toLong() else value as Long
          Type.INT32,
          Type.SINT32,
          Type.FIXED32,
          Type.SFIXED32,
          Type.UINT32 -> if (value is String) value.toInt() else value as Int
          Type.BOOL -> if (value is String) value.toBoolean() else value as Boolean
          Type.STRING -> value.toString()
          Type.GROUP -> TODO()
          Type.MESSAGE -> TODO("At time of implementation, message is not a supported wrapper type")
          Type.BYTES -> ByteString.copyFrom(Base64.getDecoder().decode(value.toString()))
          Type.ENUM -> TODO("At time of implementation, enum is not a supported wrapper type")
        }
      }
    ).build()
  }

  private fun readTimestamp(value: Any, field: Descriptors.FieldDescriptor): Message {
    return when (value) {
      is String -> Timestamps.parse(value)
      is JSONObject -> Timestamp.newBuilder()
        .setSeconds(readField(value, googleProtobufTimestamp_SecondsField.name, googleProtobufTimestamp_SecondsField) as Long)
        .setNanos(readField(value, googleProtobufTimestamp_NanosField.name, googleProtobufTimestamp_NanosField) as Int)
        .build()
      else -> throw InvalidProtocolBufferException("$value is not a valid Timestamp value")
    }
  }

  private fun readAny(value: Any, field: Descriptors.FieldDescriptor): Message {
    if (value !is JSONObject) {
      throw InvalidProtocolBufferException("$value is not a valid Any value")
    }

    val typeUrl = value.getString("@type")
    val descriptor = typeUrl?.let { typeRegistry.getDescriptorForTypeUrl(it) }
    return if (descriptor != null) {
      // DynamicMessage will let us put the "real" message in where an Any should be
      // We take advantage of this to avoid needing to serialise it here only to
      // deserialise it later on for use
      // The "correct" code would be:
      //   return com.google.protobuf.Any.newBuilder().setTypeUrl(typeUrl).setValue(readMessage(value, descriptor).toByteString()).build()
      return readMessage(value, descriptor)
    } else {
      readMessage(value, field.messageType)
    }
  }

  private fun readMessage(jsonObject: JSONObject, messageDescriptor: Descriptors.Descriptor): Message {
    val message = DynamicMessage.newBuilder(messageDescriptor)

    for (field in messageDescriptor.fields) {
      if (jsonObject.containsKey(field.name)) {
        when {
          field.isMapField -> message.setField(field, readMapField(jsonObject.getJSONObject(field.name), field))
          field.isRepeated -> message.setField(field, readRepeatedField(jsonObject.getJSONArray(field.name), field))
          else -> message.setField(field, readField(jsonObject, field.name, field))
        }
      }
    }
    return message.build()
  }

  private fun readRepeatedField(jsonArray: JSONArray, field: Descriptors.FieldDescriptor): List<*> {
    return when (field.type!!) {
      Type.DOUBLE -> jsonArray.indices.map { i -> jsonArray.getDouble(i) }
      Type.FLOAT -> jsonArray.indices.map { i -> jsonArray.getFloat(i) }
      Type.INT64,
      Type.SINT64,
      Type.FIXED64,
      Type.SFIXED64,
      Type.UINT64 -> jsonArray.indices.map { i -> jsonArray.getLong(i) }
      Type.INT32,
      Type.SINT32,
      Type.FIXED32,
      Type.SFIXED32,
      Type.UINT32 -> jsonArray.indices.map { i -> jsonArray.getInteger(i) }
      Type.BOOL -> jsonArray.indices.map { i -> jsonArray.getBoolean(i) }
      Type.STRING -> jsonArray.indices.map { i -> jsonArray.getString(i) }
      Type.GROUP -> TODO()
      Type.MESSAGE -> specialTypes[field.messageType.fullName]?.let { jsonArray.indices.map { i -> it(jsonArray[i], field) } }
        ?: jsonArray.indices.map { i -> readMessage(jsonArray.getJSONObject(i), field.messageType) }
      Type.BYTES -> jsonArray.indices.map { i -> ByteString.copyFrom(Base64.getDecoder().decode(jsonArray.getString(i))) }
      Type.ENUM -> jsonArray.indices.map { i ->
        jsonArray.getString(i).let { asString ->
          field.enumType.values.firstOrNull { it.name == asString }
            ?: asString.toIntOrNull()?.let { asInt -> field.enumType.values.firstOrNull { it.number == asInt } }
            ?: throw InvalidProtocolBufferException("$asString is not a valid ${field.enumType.fullName} value")
        }
      }
    }
  }

  private fun readMapField(jsonObject: JSONObject, field: Descriptors.FieldDescriptor): List<Message> {
    val keyField = field.messageType.findFieldByName("key")
    val valueField = field.messageType.findFieldByName("value")

    return jsonObject.keys.map { key ->
      DynamicMessage.newBuilder(field.messageType).setField(keyField, readKeyField(key, keyField)).setField(valueField, readField(jsonObject, key, valueField)).build()
    }
  }

  private fun readKeyField(key: String, field: Descriptors.FieldDescriptor): Any {
    return when (field.type!!) {
      Type.DOUBLE -> key.toDouble()
      Type.FLOAT -> key.toFloat()
      Type.INT64,
      Type.SINT64,
      Type.FIXED64,
      Type.SFIXED64,
      Type.UINT64 -> key.toLong()
      Type.INT32,
      Type.SINT32,
      Type.FIXED32,
      Type.SFIXED32,
      Type.UINT32 -> key.toInt()
      Type.BOOL -> key.toBoolean()
      Type.STRING -> key
      Type.GROUP -> TODO()
      Type.MESSAGE -> TODO("At time of implementation, message is not a supported key type")
      Type.BYTES -> ByteString.copyFrom(Base64.getDecoder().decode(key))
      Type.ENUM -> field.enumType.values.firstOrNull { it.name == key }
        ?: key.toIntOrNull()?.let { asInt -> field.enumType.values.firstOrNull { it.number == asInt } }
        ?: throw InvalidProtocolBufferException("$key is not a valid ${field.enumType.fullName} value")
    }
  }

  fun readField(parentJsonObject: JSONObject, key: String, field: Descriptors.FieldDescriptor): Any {
    return when (field.type!!) {
      Type.DOUBLE -> parentJsonObject.getDouble(key)
      Type.FLOAT -> parentJsonObject.getFloat(key)
      Type.INT64,
      Type.SINT64,
      Type.FIXED64,
      Type.SFIXED64,
      Type.UINT64 -> parentJsonObject.getLong(key)
      Type.INT32,
      Type.SINT32,
      Type.FIXED32,
      Type.SFIXED32,
      Type.UINT32 -> parentJsonObject.getInteger(key)
      Type.BOOL -> parentJsonObject.getBoolean(key)
      Type.STRING -> parentJsonObject.getString(key)
      Type.GROUP -> TODO()
      Type.MESSAGE -> specialTypes[field.messageType.fullName]?.let { it(parentJsonObject[key], field) }
        ?: readMessage(parentJsonObject.getJSONObject(key), field.messageType)
      Type.BYTES -> ByteString.copyFrom(parentJsonObject.getBytes(key))
      Type.ENUM -> parentJsonObject.getString(key).let { asString ->
        field.enumType.values.firstOrNull { it.name == asString }
          ?: asString.toIntOrNull()?.let { asInt -> field.enumType.values.firstOrNull { it.number == asInt } }
          ?: throw InvalidProtocolBufferException("$asString is not a valid ${field.enumType.fullName} value")
      }
    }
  }

  companion object {
    private val googleProtobufTimestamp_SecondsField = Timestamp.getDescriptor().findFieldByName("seconds")
    private val googleProtobufTimestamp_NanosField = Timestamp.getDescriptor().findFieldByName("nanos")
  }
}
