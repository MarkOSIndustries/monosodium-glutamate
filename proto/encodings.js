const protobuf = require('../protobuf')(require('protobufjs'))
const { SchemaConverter } = require('../protobuf.convert')
const transport = require('../grpc.transport')
const stream = require('stream')
const streams = require('../streams')
const { matchesFilter } = require('./filter')
const { inspect } = require('util')
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
    newInputStream: (inStream) => streams.readLineDelimitedJsonObjects(inStream),
    unmarshalSchemaObject: (jsonObject, {converter}) => converter.json_object_to_schema_object(jsonObject),
    unmarshalJsonObject: (jsonObject) => jsonObject,
    newOutputStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshalSchemaObject: (schemaObject, {converter, stringifyJsonObject}) => stringifyJsonObject(converter.schema_object_to_json_object(schemaObject)),
    marshalJsonObject: (jsonObject, {stringifyJsonObject}) => stringifyJsonObject(jsonObject),
  },
  lineDelimitedEncodedJson: {
    newInputStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshalSchemaObject: (line, {converter, encodingName}) => converter.json_object_to_schema_object(JSON.parse(Buffer.from(line, encodingName))),
    unmarshalJsonObject: (line, {encodingName}) => JSON.parse(Buffer.from(line, encodingName)),
    newOutputStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshalSchemaObject: (schemaObject, {converter, encodingName}) => Buffer.from(JSON.stringify(converter.schema_object_to_json_object(schemaObject))).toString(encodingName),
    marshalJsonObject: (jsonObject, {encodingName}) => Buffer.from(JSON.stringify(jsonObject)).toString(encodingName),
  },
  lineDelimitedEncodedBinary: {
    newInputStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshalSchemaObject: (line, {converter, encodingName}) => converter.string_encoded_binary_to_schema_object(line, encodingName),
    unmarshalJsonObject: (line, {converter, encodingName}) => converter.string_encoded_binary_to_json_object(line, encodingName),
    newOutputStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshalSchemaObject: (schemaObject, {converter, encodingName}) => converter.schema_object_to_string_encoded_binary(schemaObject, encodingName),
    marshalJsonObject: (jsonObject, {converter, encodingName}) => converter.json_object_to_string_encoded_binary(jsonObject, encodingName),
  },
}

class InputStreamDecoder {
  constructor(wrappedStream, schema, encoding, prefixFormat, delimiterBuffer) {
    this.inputConfig = {
      prefixFormat,
      delimiterBuffer,
      encodingName: encodings[encoding],
      converter: new SchemaConverter(schema),
    }
    this.inputFormat = formats[encodingFormats[encoding]]
    this.inStream = this.inputFormat.newInputStream(process.stdin, this.inputConfig)
  }

  _read(fnUnmarshalObject, fnHandleObject, fnHandleException) {
    this.inStream.on('data', data => {
      try {
        const obj = fnUnmarshalObject(data)
        fnHandleObject(obj)
      } catch(ex) {
        fnHandleException(ex)
      }
    })
  }

  readJsonObjects(fnHandleJsonObject, fnHandleException) {
    const that = this
    this._read(data => that.inputFormat.unmarshalJsonObject(data, that.inputConfig), fnHandleJsonObject, fnHandleException)
  }

  readSchemaObjects(fnHandleSchemaObject, fnHandleException) {
    const that = this
    this._read(data => that.inputFormat.unmarshalSchemaObject(data, that.inputConfig), fnHandleSchemaObject, fnHandleException)
  }
}

class OutputStreamEncoder {
  constructor(wrappedStream, schema, encoding, prefixFormat, delimiterBuffer, stringifyJsonObject) {
    this.outputConfig = {
      prefixFormat,
      delimiterBuffer,
      stringifyJsonObject,
      encodingName: encodings[encoding],
      converter: new SchemaConverter(schema),
    }
    this.outputFormat = formats[encodingFormats[encoding]]
    this.outStream = this.outputFormat.newOutputStream(process.stdout, this.outputConfig)
  }

  writeJsonObject(jsonObject) {
    this.outStream.write(this.outputFormat.marshalJsonObject(jsonObject, this.outputConfig))
  }

  writeSchemaObject(schemaObject) {
    this.outStream.write(this.outputFormat.marshalSchemaObject(schemaObject, this.outputConfig))
  }
}

class MockInputStreamDecoder {
  constructor(schema) {
    this.schema = schema
    this.converter = new SchemaConverter(schema)
  }

  readJsonObjects(fnHandleJsonObject) {
    setInterval(() => fnHandleJsonObject(protobuf.makeValidJsonRecord(this.schema)), 1)
  }

  readSchemaObjects(fnHandleSchemaObject) {
    setInterval(() => fnHandleSchemaObject(this.converter.json_object_to_schema_object(protobuf.makeValidJsonRecord(this.schema))), 1)
  }
}

module.exports = {
  supportedEncodings: Object.keys(encodingFormats),
  InputStreamDecoder,
  OutputStreamEncoder,
  MockInputStreamDecoder,
}
