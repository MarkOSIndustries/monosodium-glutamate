const protobuf = require('../protobuf')(require('protobufjs'))
const { SchemaConverter } = require('../protobuf.convert')
const transport = require('../grpc.transport')
const stream = require('stream')
const streams = require('../streams')
const { matchesFilter } = require('./filter')
const os = require('os')

const encodingFormats = {
  'json': 'lineDelimitedJson',
  'json_hex': 'lineDelimitedEncodedJson',
  'json_base64': 'lineDelimitedEncodedJson',
  'base64': 'lineDelimitedEncodedBinary',
  'hex': 'lineDelimitedEncodedBinary',
  'binary': 'lengthPrefixedBinary',
}
const encodings = {
  'base64': 'base64',
  'hex': 'hex',
  'json_base64': 'base64',
  'json_hex': 'hex',
}

const formats = {
  lengthPrefixedBinary: {
    newInputStream: (inStream, {prefixFormat}) => streams.readLengthPrefixedBuffers(inStream, prefixFormat),
    unmarshalSchemaObject: (binaryBuffer, {converter}) => converter.binary_buffer_to_schema_object(binaryBuffer),
    unmarshalJsonObject: (binaryBuffer, {converter}) => converter.binary_buffer_to_json_object(binaryBuffer),
    newOutputStream: (outStream, {prefixFormat}) => streams.writeLengthPrefixedBuffers(outStream, prefixFormat),
    marshalSchemaObject: (schemaObject, {converter}) => converter.schema_object_to_binary_buffer(schemaObject),
    marshalJsonObject: (jsonObject, {converter}) => converter.json_object_to_binary_buffer(jsonObject),
  },
  lineDelimitedJson: {
    newInputStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshalSchemaObject: (line, {converter}) => converter.json_object_to_schema_object(JSON.parse(line)),
    unmarshalJsonObject: (line) => JSON.parse(line),
    newOutputStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshalSchemaObject: (schemaObject, {converter, stringifyJsonObject}) => Buffer.from(stringifyJsonObject(converter.schema_object_to_json_object(schemaObject))),
    marshalJsonObject: (jsonObject, {stringifyJsonObject}) => Buffer.from(stringifyJsonObject(jsonObject)),
  },
  lineDelimitedEncodedJson: {
    newInputStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshalSchemaObject: (line, {converter, encodingName}) => converter.json_object_to_schema_object(JSON.parse(Buffer.from(line.toString(), encodingName))),
    unmarshalJsonObject: (line, {encodingName}) => JSON.parse(Buffer.from(line.toString(), encodingName)),
    newOutputStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshalSchemaObject: (schemaObject, {converter, encodingName}) => Buffer.from(Buffer.from(JSON.stringify(converter.schema_object_to_json_object(schemaObject))).toString(encodingName)),
    marshalJsonObject: (jsonObject, {encodingName}) => Buffer.from(Buffer.from(JSON.stringify(jsonObject)).toString(encodingName)),
  },
  lineDelimitedEncodedBinary: {
    newInputStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshalSchemaObject: (line, {converter, encodingName}) => converter.string_encoded_binary_to_schema_object(line.toString(), encodingName),
    unmarshalJsonObject: (line, {converter, encodingName}) => converter.string_encoded_binary_to_json_object(line.toString(), encodingName),
    newOutputStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshalSchemaObject: (schemaObject, {converter, encodingName}) => Buffer.from(converter.schema_object_to_string_encoded_binary(schemaObject, encodingName)),
    marshalJsonObject: (jsonObject, {converter, encodingName}) => Buffer.from(converter.json_object_to_string_encoded_binary(jsonObject, encodingName)),
  },
}

class InputStreamDecoder {
  constructor(wrappedStream, schema, encoding, prefixFormat, delimiterBuffer) {
    this.wrappedStream = wrappedStream
    this.schema = schema
    this.inputConfig = {
      prefixFormat,
      delimiterBuffer,
      encodingName: encodings[encoding],
      converter: new SchemaConverter(schema),
    }
    this.inputFormat = formats[encodingFormats[encoding]]
  }

  getSchemaName() {
    return this.schema.fullName
  }

  makeInputStream() {
    return this.inputFormat.newInputStream(this.wrappedStream, this.inputConfig)
  }

  unmarshalJsonObject(data) {
    return this.inputFormat.unmarshalJsonObject(data, this.inputConfig)
  }

  unmarshalSchemaObject(data) {
    return this.inputFormat.unmarshalSchemaObject(data, this.inputConfig)
  }
}

class OutputStreamEncoder {
  constructor(wrappedStream, schema, encoding, prefixFormat, delimiterBuffer, stringifyJsonObject) {
    this.wrappedStream = wrappedStream
    this.schema = schema
    this.outputConfig = {
      prefixFormat,
      delimiterBuffer,
      stringifyJsonObject,
      encodingName: encodings[encoding],
      converter: new SchemaConverter(schema),
    }
    this.outputFormat = formats[encodingFormats[encoding]]
  }

  getSchemaName() {
    return this.schema.fullName
  }

  makeOutputStream() {
    return this.outputFormat.newOutputStream(this.wrappedStream, this.outputConfig)
  }

  marshalJsonObject(jsonObject) {
    return this.outputFormat.marshalJsonObject(jsonObject, this.outputConfig)
  }

  marshalSchemaObject(schemaObject) {
    return this.outputFormat.marshalSchemaObject(schemaObject, this.outputConfig)
  }
}

class MockInputStreamDecoder {
  constructor(schema) {
    this.schema = schema
    this.converter = new SchemaConverter(schema)
  }

  getSchemaName() {
    return this.schema.fullName
  }

  makeInputStream() {
    const self = this

    let closed = false

    const readable = new stream.Readable({
      objectMode: true,

      read(size) {
        setTimeout(() => {
          if(!closed) {
            this.push('msg')
          }
        }, 1)
      }
    })

    process.on('SIGINT', function() {
      closed = true
      readable.push(null)
    })

    return readable
  }

  unmarshalJsonObject(data) {
    return protobuf.makeValidJsonRecord(this.schema)
  }

  unmarshalSchemaObject(data) {
    return this.converter.json_object_to_schema_object(protobuf.makeValidJsonRecord(this.schema))
  }
}

module.exports = {
  supportedEncodings: Object.keys(encodingFormats),
  InputStreamDecoder,
  OutputStreamEncoder,
  MockInputStreamDecoder,
}
