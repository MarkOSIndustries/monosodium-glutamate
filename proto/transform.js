const protobuf = require('../protobuf')(require('protobufjs'))
const { SchemaConverter } = require('../protobuf.convert')
const stream = require('stream')
const streams = require('../streams')
const { matchesFilter } = require('./filter')
const { inspect } = require('util')
module.exports = {
  transform,
}

const formats = {
  'json': 'lineDelimitedJson',
  'base64': 'lineDelimitedEncodedBinary',
  'hex': 'lineDelimitedEncodedBinary',
  'binary': 'lengthPrefixedBinary',
  'generator': 'generator',
  'spam': 'generator',
}
const encodings = {
  'base64': 'base64',
  'hex': 'hex',
}

const inputFormats = {
  lengthPrefixedBinary: {
    newStream: (inStream, {prefixFormat}) => streams.readLengthPrefixedBuffers(inStream, prefixFormat),
    unmarshal: (binaryBuffer, {converter}) => converter.binary_buffer_to_json_object(binaryBuffer),
  },
  lineDelimitedJson: {
    newStream: (inStream) => streams.readLineDelimitedJsonObjects(inStream),
    unmarshal: (jsonObject) => jsonObject,
  },
  lineDelimitedEncodedBinary: {
    newStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshal: (line, {converter, encodingName}) => converter.string_encoded_binary_to_json_object(line, encodingName),
  },
  generator: {
    newStream: (inStream, {converter}) => {
      const outStream = new stream.Writable()
      setInterval(() => outStream.emit('data', protobuf.makeValidJsonRecord(converter.schema)), 1)
      return outStream
    },
    unmarshal: (jsonObject) => jsonObject,
  },
}

const outputFormats = {
  lengthPrefixedBinary: {
    newStream: (outStream, {prefixFormat}) => streams.writeLengthPrefixedBuffers(outStream, prefixFormat),
    marshal: (jsonObject, {converter}) => converter.json_object_to_binary_buffer(jsonObject),
  },
  lineDelimitedJson: {
    newStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshal: (jsonObject, {stringifyJsonObject}) => stringifyJsonObject(jsonObject),
  },
  lineDelimitedEncodedBinary: {
    newStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshal: (jsonObject, {converter, encodingName}) => converter.json_object_to_string_encoded_binary(jsonObject, encodingName),
  },
}

function transform({input, output, schema, prefix, encoding, delimiter, protobufs, filter, shape, template}) {
  const transformConfig = {
    prefixFormat: prefix,
    delimiterBuffer: delimiter,
    stringifyJsonObject: template,
    converter: new SchemaConverter(protobuf.loadDirectory(protobufs).lookupType(schema)),
  }

  const inputConfig = Object.assign({encodingName: encodings[input]}, transformConfig)
  const inputFormat = inputFormats[formats[input]]

  const outputConfig = Object.assign({encodingName: encodings[output]}, transformConfig)
  const outputFormat = outputFormats[formats[output]]

  const inStream = inputFormat.newStream(process.stdin, inputConfig)
  const outStream = outputFormat.newStream(process.stdout, outputConfig)

  inStream.on('data', data => {
    const jsonObject = inputFormat.unmarshal(data, inputConfig)
    if(filter(jsonObject)) {
      const shapedJsonObject = shape(jsonObject)
      outStream.write(outputFormat.marshal(shapedJsonObject, outputConfig))
    }
  })

  process.stdin.on('end', () => { process.exit() })
}