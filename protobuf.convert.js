class Converters {
  constructor(schemas) {
    this.schemas = schemas
  }

  forSchema(schemaName) {
    return new SchemaConverter(schemas.lookupType(schemaName))
  }
}

class SchemaConverter {
  constructor(schema) {
    this.schema = schema
  }

  binary_buffer_to_schema_object(binaryBuffer) {
    return this.schema.decode(binaryBuffer)
  }

  binary_buffer_to_string_encoded_binary(binaryBuffer, encoding) {
    return binaryBuffer.toString(encoding)
  }

  schema_object_to_json_object(schemaObject) {
    return this.schema.toObject(schemaObject, {
          keepCase: true,
          longs: Number,
          enums: String,
          bytes: String,
          json: true,
          defaults: true,
          oneofs: true,
        })
  }

  schema_object_to_binary_buffer(schemaObject) {
    return this.schema.encode(schemaObject).finish()
  }

  json_object_to_schema_object(jsonObject) {
    return this.schema.fromObject(jsonObject)
  }

  string_encoded_binary_to_binary_buffer(stringEncodedBinary, encoding) {
    return Buffer.from(stringEncodedBinary, encoding)
  }

  binary_buffer_to_json_object(binaryBuffer) {
    return this.schema_object_to_json_object(this.binary_buffer_to_schema_object(binaryBuffer))
  }

  schema_object_to_string_encoded_binary(schemaObject, encoding) {
    return this.binary_buffer_to_string_encoded_binary(this.schema_object_to_binary_buffer(schemaObject), encoding)
  }

  string_encoded_binary_to_schema_object(stringEncodedBinary, encoding) {
    return this.binary_buffer_to_schema_object(this.string_encoded_binary_to_binary_buffer(stringEncodedBinary, encoding))
  }

  string_encoded_binary_to_json_object(stringEncodedBinary, encoding) {
    return this.binary_buffer_to_json_object(this.string_encoded_binary_to_binary_buffer(stringEncodedBinary, encoding))
  }

  json_object_to_binary_buffer(jsonObject) {
    return this.schema_object_to_binary_buffer(this.json_object_to_schema_object(jsonObject))
  }

  json_object_to_string_encoded_binary(jsonObject, encoding) {
    return this.binary_buffer_to_string_encoded_binary(this.json_object_to_binary_buffer(jsonObject), encoding)
  }
}

module.exports = {
  Converters,
  SchemaConverter,
}
