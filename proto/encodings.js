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
    this.inputConfig = {
      prefixFormat,
      delimiterBuffer,
      encodingName: encodings[encoding],
      converter: new SchemaConverter(schema),
    }
    this.inputFormat = formats[encodingFormats[encoding]]
  }

  makeInputStream() {
    return this.inputFormat.newInputStream(this.wrappedStream, this.inputConfig)
  }

  unmarshalJsonObject(data) {
    return this.inputFormat.unmarshalJsonObject(data, this.inputConfig)
  }

  streamJsonObjects(fnHandleException) {
    const that = this
    const transform = new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform(data, encoding, done) {
        try {
          this.push(that.unmarshalJsonObject(data))
        } catch(ex) {
          fnHandleException(ex)
        }
        done()
      }
    })

    this.makeInputStream().pipe(transform)

    return transform
  }

  unmarshalSchemaObject(data) {
    return this.inputFormat.unmarshalSchemaObject(data, this.inputConfig)
  }

  streamSchemaObjects(fnHandleException) {
    const that = this
    const transform = new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform(data, encoding, done) {
        try {
          this.push(that.unmarshalSchemaObject(data))
        } catch(ex) {
          fnHandleException(ex)
        }
        done()
      }
    })

    this.makeInputStream().pipe(transform)

    return transform
  }
}

class OutputStreamEncoder {
  constructor(wrappedStream, schema, encoding, prefixFormat, delimiterBuffer, stringifyJsonObject) {
    this.wrappedStream = wrappedStream
    this.outputConfig = {
      prefixFormat,
      delimiterBuffer,
      stringifyJsonObject,
      encodingName: encodings[encoding],
      converter: new SchemaConverter(schema),
    }
    this.outputFormat = formats[encodingFormats[encoding]]
  }

  makeOutputStream() {
    return this.outputFormat.newOutputStream(this.wrappedStream, this.outputConfig)
  }

  marshalJsonObject(jsonObject) {
    return this.outputFormat.marshalJsonObject(jsonObject, this.outputConfig)
  }

  streamJsonObjects() {
    const that = this
    const transform = new stream.Transform({
      writableObjectMode: true,
      
      transform(jsonObject, encoding, done) {
        this.push(that.marshalJsonObject(jsonObject))
        done()
      }
    })

    transform.pipe(this.makeOutputStream())

    return transform
  }

  marshalSchemaObject(schemaObject) {
    return this.outputFormat.marshalSchemaObject(schemaObject, this.outputConfig)
  }

  streamSchemaObjects() {
    const that = this
    const transform = new stream.Transform({
      writableObjectMode: true,
      
      transform(schemaObject, encoding, done) {
        this.push(that.marshalSchemaObject(schemaObject))
        done()
      }
    })

    transform.pipe(this.makeOutputStream())
    
    return transform
  }
}

class MockInputStreamDecoder {
  constructor(schema) {
    this.schema = schema
    this.converter = new SchemaConverter(schema)
  }

  // TODO: Improve this to match the real InputStreamDecoder (new composable methods)

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
