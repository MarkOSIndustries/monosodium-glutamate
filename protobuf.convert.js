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
    return schemaObject.toJSON()
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

// TODO: break the below into delimited stream and length-prefixed stream converters

  stream_to_buffer(stream) {
    return new Promise((resolve,reject) => {
      const chunks = [];
      stream.on('data', function(chunk) { chunks.push(chunk); });
      stream.on('end', function() {
          const buffer = Buffer.concat(chunks);
          resolve(buffer);
      });
    });
  }

  stream_to_string(stream, encoding) {
    return stream_to_buffer(stream)
      .then(buffer => buffer.toString(encoding));
  }
}

module.exports = {
  Converters,
  SchemaConverter,
}
