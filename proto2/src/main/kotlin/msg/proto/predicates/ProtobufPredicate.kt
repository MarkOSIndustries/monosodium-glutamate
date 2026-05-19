package msg.proto.predicates

import com.github.ajalt.clikt.core.ProgramResult
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import msg.predicates.And
import msg.predicates.Comparison
import msg.predicates.ComparisonOp
import msg.predicates.Not
import msg.predicates.Or
import msg.predicates.Predicate

object ProtobufPredicate {
  fun build(
    messageDescriptor: Descriptors.Descriptor,
    predicateAst: Predicate,
  ): (Message) -> Boolean =
    when (predicateAst) {
      is Not -> {
        val invertPredicate = build(messageDescriptor, predicateAst.predicate);
        { message -> !invertPredicate(message) }
      }
      is And -> {
        val leftPredicate = build(messageDescriptor, predicateAst.left)
        val rightPredicate = build(messageDescriptor, predicateAst.right);
        { message -> leftPredicate(message) && rightPredicate(message) }
      }
      is Or -> {
        val leftPredicate = build(messageDescriptor, predicateAst.left)
        val rightPredicate = build(messageDescriptor, predicateAst.right);
        { message -> leftPredicate(message) || rightPredicate(message) }
      }
      is Comparison -> {
        val (fieldDesc, _) =
          predicateAst.dataPath.pathElements.fold(
            (null as Descriptors.FieldDescriptor? to messageDescriptor),
          ) { node, element ->
            val (_, msgDesc) = node
            val fieldDesc = msgDesc.fields.find { it.name == element }
            if (fieldDesc == null) {
              System.err.println(
                "Predicate references non-existent schema field: ${predicateAst.dataPath}",
              )
              throw ProgramResult(1)
            }
            if (fieldDesc.type == Type.MESSAGE) {
              (null to fieldDesc.messageType)
            } else {
              (fieldDesc to messageDescriptor)
            }
          }
        val typedComparator: Any = parseFromString(fieldDesc!!, predicateAst.comparator)
        val comparisonCheck: (Int) -> Boolean =
          when (predicateAst.op) {
            ComparisonOp.AnyEquals,
            ComparisonOp.AllEquals,
            -> { it -> it == 0 }
            ComparisonOp.AnyNotEquals,
            ComparisonOp.AllNotEquals,
            -> { it -> it != 0 }
            ComparisonOp.LessThan -> { it -> it < 0 }
            ComparisonOp.GreaterThan -> { it -> it > 0 }
            ComparisonOp.LessThanOrEqual -> { it -> it <= 0 }
            ComparisonOp.GreaterThanOrEqual -> { it -> it >= 0 }
          }

        val compare: (List<*>) -> Boolean =
          when (predicateAst.op) {
            ComparisonOp.AllEquals,
            ComparisonOp.AllNotEquals,
            -> { values -> values.all { comparisonCheck(compareSingle(fieldDesc, it, typedComparator)) } }
            ComparisonOp.AnyEquals,
            ComparisonOp.AnyNotEquals,
            -> { values -> values.any { comparisonCheck(compareSingle(fieldDesc, it, typedComparator)) } }
            else -> { values -> values.any { comparisonCheck(compareSingle(fieldDesc, it, typedComparator)) } }
          }

        { message: Message ->
          val (_, toCompare) =
            predicateAst.dataPath.pathElements.fold(messageDescriptor to listOf(message) as List<*>) { node, element ->
              val (msgDesc, msgs) = node
              val fieldDesc = (msgDesc.fields.find { it.name == element })!!
              val nextMessageDesc = if (fieldDesc.type == Type.MESSAGE) fieldDesc.messageType else messageDescriptor
              if (fieldDesc.isRepeated) {
                nextMessageDesc to msgs.flatMap { msg -> (msg as Message).getField(fieldDesc) as List<*> }
              } else {
                nextMessageDesc to msgs.map { msg -> (msg as Message).getField(fieldDesc) }
              }
            }

          compare(toCompare)
        }
      }
    }

  private fun <T> compareSingle(
    fieldDescriptor: Descriptors.FieldDescriptor,
    value: T,
    comparator: T,
  ): Int =
    when (fieldDescriptor.type) {
      Type.DOUBLE -> (value as Double).compareTo(comparator as Double)
      Type.FLOAT -> (value as Float).compareTo(comparator as Float)
      Type.INT64,
      Type.SINT64,
      Type.FIXED64,
      Type.SFIXED64,
      Type.UINT64,
      -> (value as Long).compareTo(comparator as Long)
      Type.INT32,
      Type.SINT32,
      Type.FIXED32,
      Type.SFIXED32,
      Type.UINT32,
      -> (value as Int).compareTo(comparator as Int)
      Type.BOOL -> (value as Boolean).compareTo(comparator as Boolean)
      Type.STRING -> (value as String).compareTo(comparator as String)
      Type.GROUP -> TODO()
      Type.MESSAGE -> TODO()
      Type.BYTES -> if ((value as ByteString).equals(comparator as ByteString)) 0 else 1
      Type.ENUM -> if ((value as EnumValueDescriptor).equals(comparator as EnumValueDescriptor)) 0 else 1
    }

  private fun <T> parseFromString(
    fieldDescriptor: Descriptors.FieldDescriptor,
    valueAsString: String,
  ): T {
    try {
      return when (fieldDescriptor.type) {
        Type.DOUBLE -> valueAsString.toDouble()
        Type.FLOAT -> valueAsString.toFloat()
        Type.INT64,
        Type.SINT64,
        Type.FIXED64,
        Type.SFIXED64,
        Type.UINT64,
        -> valueAsString.toLong()

        Type.INT32,
        Type.SINT32,
        Type.FIXED32,
        Type.SFIXED32,
        Type.UINT32,
        -> valueAsString.toInt()

        Type.BOOL -> valueAsString.toBoolean()
        Type.STRING -> valueAsString
        Type.GROUP -> TODO()
        Type.MESSAGE -> TODO()
        Type.BYTES -> ByteString.copyFromUtf8(valueAsString)
        Type.ENUM ->
          valueAsString.let { asString ->
            fieldDescriptor.enumType.values.firstOrNull { it.name == asString }
              ?: asString
                .toIntOrNull()
                ?.let { asInt -> fieldDescriptor.enumType.values.firstOrNull { it.number == asInt } }
              ?: throw InvalidProtocolBufferException("$asString is not a valid ${fieldDescriptor.enumType.fullName} value")
          }
      } as T
    } catch (e: Exception) {
      System.err.println("$valueAsString is not a valid ${fieldDescriptor.type} for field ${fieldDescriptor.name}")
      throw ProgramResult(1)
    }
  }
}
