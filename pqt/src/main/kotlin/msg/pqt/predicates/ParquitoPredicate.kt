package msg.pqt.predicates

import com.github.ajalt.clikt.core.ProgramResult
import com.markosindustries.parquito.ParquetPredicates
import com.markosindustries.parquito.ParquetSchemaNode
import com.markosindustries.parquito.RowGroupReader
import com.markosindustries.parquito.predicates.ParquetPredicate
import msg.predicates.And
import msg.predicates.Comparison
import msg.predicates.ComparisonOp
import msg.predicates.Not
import msg.predicates.Or
import msg.predicates.Predicate
import org.apache.parquet.format.ConvertedType
import org.apache.parquet.format.LogicalType
import org.apache.parquet.format.Type
import java.nio.ByteBuffer
import java.util.UUID

object ParquitoPredicate {
  fun build(
    rowGroupReader: RowGroupReader,
    schema: ParquetSchemaNode.Root,
    predicateAst: Predicate,
  ): ParquetPredicate =
    when (predicateAst) {
      is And ->
        ParquetPredicates.intersection(
          build(rowGroupReader, schema, predicateAst.left),
          build(rowGroupReader, schema, predicateAst.right),
        )
      is Or -> ParquetPredicates.union(build(rowGroupReader, schema, predicateAst.left), build(rowGroupReader, schema, predicateAst.right))
      is Not -> ParquetPredicates.not(build(rowGroupReader, schema, predicateAst.predicate))
      is Comparison -> {
        val schemaPath = schema.parsePathElements(predicateAst.dataPath.pathElements)
        val schemaNode = schema.getChild(schemaPath)
        val columnIndex =
          rowGroupReader.getColumnChunkIndexForSchemaPath(schemaPath).orElseGet {
            System.err.println("$schemaPath is not a valid column.")
            when (schemaNode.convertedType) {
              ConvertedType.MAP -> System.err.println("It is a MAP - try $schemaPath.key_value.key or $schemaPath.key_value.value")
              ConvertedType.LIST -> System.err.println("It is a LIST - try $schemaPath.list.element")
              else -> {}
            }
            throw ProgramResult(1)
          }
        val columnType =
          rowGroupReader
            .rowGroupHeader()
            .columns[columnIndex]
            .meta_data.type!!
        val typedComparator = parseFromString(columnType, schemaNode.logicalType, predicateAst.comparator)
        when (predicateAst.op) {
          ComparisonOp.AnyEquals -> ParquetPredicates.anyEquals(rowGroupReader, typedComparator, schemaPath)
          ComparisonOp.AllEquals -> ParquetPredicates.allEquals(rowGroupReader, typedComparator, schemaPath)
          ComparisonOp.AnyNotEquals -> ParquetPredicates.anyNotEquals(rowGroupReader, typedComparator, schemaPath)
          ComparisonOp.AllNotEquals -> ParquetPredicates.noneEquals(rowGroupReader, typedComparator, schemaPath)
          ComparisonOp.LessThan -> ParquetPredicates.anyLessThan(rowGroupReader, typedComparator, schemaPath)
          ComparisonOp.GreaterThan -> ParquetPredicates.anyGreaterThan(rowGroupReader, typedComparator, schemaPath)
          ComparisonOp.LessThanOrEqual -> ParquetPredicates.anyLessThanOrEqual(rowGroupReader, typedComparator, schemaPath)
          ComparisonOp.GreaterThanOrEqual -> ParquetPredicates.anyGreaterThanOrEqual(rowGroupReader, typedComparator, schemaPath)
        }
      }
    }

  private fun parseFromString(
    columnType: Type,
    logicalType: LogicalType?,
    valueAsString: String,
  ): Any =
    when (columnType) {
      Type.BOOLEAN -> valueAsString.toBoolean()
      Type.INT32 -> valueAsString.toInt()
      Type.INT64 -> valueAsString.toLong()
      Type.INT96 -> TODO("Int96 not currently supported")
      Type.FLOAT -> valueAsString.toFloat()
      Type.DOUBLE -> valueAsString.toDouble()
      Type.FIXED_LEN_BYTE_ARRAY, Type.BYTE_ARRAY ->
        when (logicalType?.setField) {
          LogicalType._Fields.ENUM,
          LogicalType._Fields.JSON,
          LogicalType._Fields.STRING,
          -> valueAsString
          LogicalType._Fields.UUID -> UUID.fromString(valueAsString)
          else -> ByteBuffer.wrap(valueAsString.toByteArray())
        }
    }
}
