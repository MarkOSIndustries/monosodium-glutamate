const parquet = require('parquetjs-lite')

module.exports = {
  protobufSchemaToParquetSpec,
}

function protobufSchemaToParquetSpec(protobufSchema, {compression, enumsAsStrings, sparkCompatibleMode}) {
  const {schema,protoToParquet,parquetToProto} = _protobufSchemaToParquetSpec(protobufSchema, [], false, compression, enumsAsStrings, sparkCompatibleMode)
  return {
    schema: schema.fields,
    protoToParquet,
    parquetToProto,
  }
}

function _protobufSchemaToParquetSpec(messageType, messageTypeBlacklist, isRepeatedField, compression, enumsAsStrings, sparkCompatibleMode) {
  switch(messageType.constructor.name) {
      case 'Enum':
        if(enumsAsStrings) {
          return {
            schema: { type: 'UTF8', optional: true, compression },
            protoToParquet: x => x,
            parquetToProto: x => x,
          }
        } else {
          return {
            schema: { type: 'INT64', optional: true, compression },
            protoToParquet: x => messageType.values[x],
            parquetToProto: x => Object.keys(messageType.values)[Object.values(messageType.values).indexOf(Number(x))],
          }
        }
        return result
      case 'Type':
      default: // assume anything we don't understand is a message and hope for the best
        if(!messageType.fields) {
          return {}
        }
        const protoToParquetTransforms = []
        const parquetToProtoTransforms = []
        const fields = Object.assign({}, ...Object.keys(messageType.fields).map(fieldKey => {
          const field = messageType.fields[fieldKey]

          if(field.keyType) {
            return {
              [field.name]: {
                repeated: true,
                fields: {
                  key: { type: _protoToParquetPrimitive(field.keyType), optional: true, compression },
                  value: { type: _protoToParquetPrimitive(field.type), optional: true, compression },
                }
              }
            }
          }

          if(field.resolvedType) {
            // Prevent recursing to generate a type we saw further up the chain
            if(new Set(messageTypeBlacklist).has(field.resolvedType)) {
              return {}
            }

            const nested = _protobufSchemaToParquetSpec(field.resolvedType, [...messageTypeBlacklist, messageType], field.repeated, compression, enumsAsStrings, sparkCompatibleMode)
            protoToParquetTransforms.push((x) => {
              if(x && field.name in x) {
                x[field.name] = nested.protoToParquet(x[field.name])
              }
              return x
            })
            parquetToProtoTransforms.push((x) => {
              if(x && field.name in x) {
                x[field.name] = nested.parquetToProto(x[field.name])
              }
              return x
            })

            return { [field.name]: nested.schema }
          }

          if(field.type === "bytes") {
            protoToParquetTransforms.push((x) => {
              if(x && field.name in x) {
                x[field.name] = Buffer.from(x[field.name], 'base64')
              }
              return x
            })

            parquetToProtoTransforms.push((x) => {
              if(x && field.name in x) {
                x[field.name] = x[field.name].toString('base64')
              }
              return x
            })
          }

          const parquetPrimitiveType = _protoToParquetPrimitive(field.type)
          if(_isParquetNumberType(parquetPrimitiveType)) {
            // parquetjs-lite uses BigInt for these, we need them as Numbers
            parquetToProtoTransforms.push((x) => {
              if(x && field.name in x) {
                x[field.name] = Number(x[field.name])
              }
              return x
            })
          }
          return { [field.name]: { type: parquetPrimitiveType, repeated: field.repeated, optional: true, compression } }
        }))

        const schema = (sparkCompatibleMode && isRepeatedField) ? {
            repeated: false,
            optional: true,
            compression,
            fields: {
              list: {
                repeated: true,
                optional: true,
                compression,
                fields: {
                  element: { repeated: false, optional: false, compression, fields }
                }
              }
            },
          } : {
            repeated: isRepeatedField,
            optional: true,
            compression,
            fields,
          }

        return {
          schema,
          protoToParquet: x => {
            for(const protoToParquetTransform of protoToParquetTransforms) {
              if(isRepeatedField) {
                x = x.map(protoToParquetTransform)
              } else {
                x = protoToParquetTransform(x)
              }
            }
            if(sparkCompatibleMode && isRepeatedField) {
              x = { list: x.map(element => ({ element })) }
            }
            return x
          },
          parquetToProto: x => {
            if(sparkCompatibleMode && isRepeatedField) {
              x = x.list.map(e => e.element)
            }
            for(const parquetToProtoTransform of parquetToProtoTransforms) {
              if(isRepeatedField) {
                x = x.map(parquetToProtoTransform)
              } else {
                x = parquetToProtoTransform(x)
              }
            }
            return x
          },
        }
    }
}

function _protoToParquetPrimitive(fieldType, fieldName) {
  switch(fieldType) {
    case "bool":
      return 'BOOLEAN'
    case "string":
      return 'UTF8'
    case "bytes":
      return 'BYTE_ARRAY'
    case "double":
      return 'DOUBLE'
    case "float":
      return 'FLOAT'
    case "int32":
    case "uint32":
    case "sint32":
    case "fixed32":
    case "sfixed32":
      return 'INT32'
    case "int64":
    case "uint64":
    case "sint64":
    case "fixed64":
    case "sfixed64":
      return 'INT64'
    default:
      // assume anything else is a number :)
      return 'INT64'
  }
}

function _isParquetNumberType(parquetPrimitiveType) {
  switch(parquetPrimitiveType) {
    case "DOUBLE":
    case "FLOAT":
    case "INT32":
    case "INT64":
      return true
    default:
      return false
  }
}