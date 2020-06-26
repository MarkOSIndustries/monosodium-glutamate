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
    newInputStream: (inStream) => streams.readLineDelimitedJsonObjects(inStream),
    unmarshalSchemaObject: (jsonObject, {converter}) => converter.json_object_to_schema_object(jsonObject),
    unmarshalJsonObject: (jsonObject) => jsonObject,
    newOutputStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshalSchemaObject: (schemaObject, {converter, stringifyJsonObject}) => stringifyJsonObject(converter.schema_object_to_json_object(schemaObject)),
    marshalJsonObject: (jsonObject, {stringifyJsonObject}) => stringifyJsonObject(jsonObject),
  },
  lineDelimitedEncodedJson: {
    newInputStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshalSchemaObject: (line, {converter, encodingName}) => converter.json_object_to_schema_object(JSON.parse(Buffer.from(line.toString(), encodingName))),
    unmarshalJsonObject: (line, {encodingName}) => JSON.parse(Buffer.from(line.toString(), encodingName)),
    newOutputStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshalSchemaObject: (schemaObject, {converter, encodingName}) => Buffer.from(JSON.stringify(converter.schema_object_to_json_object(schemaObject))).toString(encodingName),
    marshalJsonObject: (jsonObject, {encodingName}) => Buffer.from(JSON.stringify(jsonObject)).toString(encodingName),
  },
  lineDelimitedEncodedBinary: {
    newInputStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshalSchemaObject: (line, {converter, encodingName}) => converter.string_encoded_binary_to_schema_object(line.toString(), encodingName),
    unmarshalJsonObject: (line, {converter, encodingName}) => converter.string_encoded_binary_to_json_object(line.toString(), encodingName),
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
    this.inStream = this.inputFormat.newInputStream(wrappedStream, this.inputConfig)
  }

  streamJsonObjects(fnHandleException) {
    const that = this
    const transform = new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform(data, encoding, done) {
        try {
          this.push(that.inputFormat.unmarshalJsonObject(data, that.inputConfig))
        } catch(ex) {
          fnHandleException(ex)
        }
        done()
      }
    })

    this.inStream.pipe(transform)

    return transform
  }

  streamSchemaObjects(fnHandleException) {
    const that = this
    const transform = new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform(data, encoding, done) {
        try {
          this.push(that.inputFormat.unmarshalSchemaObject(data, that.inputConfig))
        } catch(ex) {
          fnHandleException(ex)
        }
        done()
      }
    })

    this.inStream.pipe(transform)

    return transform
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
    this.outStream = this.outputFormat.newOutputStream(wrappedStream, this.outputConfig)
  }

  streamJsonObjects() {
    const that = this
    const transform = new stream.Transform({
      writableObjectMode: true,
      
      transform(jsonObject, encoding, done) {
        this.push(that.outputFormat.marshalJsonObject(jsonObject, that.outputConfig))
        done()
      }
    })

    transform.pipe(this.outStream)

    return transform
  }

  streamSchemaObjects() {
    const that = this
    const transform = new stream.Transform({
      writableObjectMode: true,
      
      transform(schemaObject, encoding, done) {
        this.push(that.outputFormat.marshalSchemaObject(schemaObject, that.outputConfig))
        done()
      }
    })

    transform.pipe(this.outStream)
    
    return transform
  }
}

class MockInputStreamDecoder {
  constructor(schema) {
    this.schema = schema
    this.converter = new SchemaConverter(schema)
  }

  streamJsonObjects() {
    const that = this

    let closed = false

    const readable = new stream.Readable({
      objectMode: true,

      read(size) {
        setTimeout(() => {
          if(!closed) {
            this.push(protobuf.makeValidJsonRecord(that.schema))
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

  streamSchemaObjects() {
    const that = this

    let closed = false

    const readable = new stream.Readable({
      objectMode: true,

      read(size) {
        if(closed) {
          this.push(null)
        } else {
          this.push(that.converter.json_object_to_schema_object(protobuf.makeValidJsonRecord(that.schema)))
        }
      }
    })

    process.on('SIGINT', function() {
      closed = true
    })

    return readable
  }
}

module.exports = {
  supportedEncodings: Object.keys(encodingFormats),
  InputStreamDecoder,
  OutputStreamEncoder,
  MockInputStreamDecoder,
}
