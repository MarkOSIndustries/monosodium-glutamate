const protobuf = require('../protobuf')(require('protobufjs'))
const { SchemaConverter } = require('../protobuf.convert')
const transport = require('../grpc.transport')
const stream = require('stream')
const streams = require('../streams')
const { matchesFilter } = require('./filter')
const { inspect } = require('util')
const os = require('os')

module.exports = {
  invoke,
}

const formats = {
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

const inputFormats = {
  lengthPrefixedBinary: {
    newStream: (inStream, {prefixFormat}) => streams.readLengthPrefixedBuffers(inStream, prefixFormat),
    unmarshal: (binaryBuffer, {converter}) => converter.binary_buffer_to_schema_object(binaryBuffer),
  },
  lineDelimitedJson: {
    newStream: (inStream) => streams.readLineDelimitedJsonObjects(inStream),
    unmarshal: (jsonObject, {converter}) => converter.json_object_to_schema_object(jsonObject),
  },
  lineDelimitedEncodedJson: {
    newStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshal: (line, {converter, encodingName}) => converter.json_object_to_schema_object(JSON.parse(Buffer.from(line, encodingName))),
  },
  lineDelimitedEncodedBinary: {
    newStream: (inStream) => streams.readUTF8Lines(inStream),
    unmarshal: (line, {converter, encodingName}) => converter.string_encoded_binary_to_schema_object(line, encodingName),
  },
}

const outputFormats = {
  lengthPrefixedBinary: {
    newStream: (outStream, {prefixFormat}) => streams.writeLengthPrefixedBuffers(outStream, prefixFormat),
    marshal: (schemaObject, {converter}) => converter.schema_object_to_binary_buffer(schemaObject),
  },
  lineDelimitedJson: {
    newStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshal: (schemaObject, {converter, stringifyJsonObject}) => stringifyJsonObject(converter.schema_object_to_json_object(schemaObject)),
  },
  lineDelimitedEncodedJson: {
    newStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshal: (schemaObject, {converter, encodingName}) => Buffer.from(JSON.stringify(converter.schema_object_to_json_object(schemaObject))).toString(encodingName),
  },
  lineDelimitedEncodedBinary: {
    newStream: (outStream, {delimiterBuffer}) => streams.writeDelimited(outStream, delimiterBuffer),
    marshal: (schemaObject, {converter, encodingName}) => converter.schema_object_to_string_encoded_binary(schemaObject, encodingName),
  },
}

function invoke({input, output, service, method, host, port, prefix, encoding, delimiter, protobufs, template}) {
  const methodObject = protobuf.describeServiceMethods(tryToLoadService(protobufs, service))[method]

  const transformConfig = {
    prefixFormat: prefix,
    delimiterBuffer: delimiter,
    stringifyJsonObject: template,
  }

  const inputConfig = Object.assign({
    encodingName: encodings[input],
    converter: new SchemaConverter(methodObject.requestType)
  }, transformConfig)
  const inputFormat = inputFormats[formats[input]]

  const outputConfig = Object.assign({
    encodingName: encodings[output],
    converter: new SchemaConverter(methodObject.responseType)
  }, transformConfig)
  const outputFormat = outputFormats[formats[output]]

  const inStream = inputFormat.newStream(process.stdin, inputConfig)
  const outStream = outputFormat.newStream(process.stdout, outputConfig)

  const channelManager = new transport.ChannelManager()

  var requestsSent = 0
  var responsesReceived = 0
  var requestsCompleted = 0
  inStream.on('data', data => {
    try {
      const requestSchemaObject = inputFormat.unmarshal(data, inputConfig)
      requestsSent = requestsSent + 1
      const responseStream = methodObject.invokeWith(channelManager.getChannel(host, port), requestSchemaObject)
      responseStream.on('data', responseSchemaObject => {
        responsesReceived = responsesReceived + 1
        outStream.write(outputFormat.marshal(responseSchemaObject, outputConfig))
      })
      responseStream.on('end', () => {
        requestsCompleted = requestsCompleted + 1
        if(requestsCompleted === requestsSent && !process.stdin.isTTY) exit()
      })
      responseStream.on('error', error => { console.error(error) })
    } catch(ex) {
      console.error(ex)
    }
  })

  const exit = () => {
    process.stderr.write(`Sent ${requestsSent} requests and received ${responsesReceived} responses${os.EOL}`)
    process.exit()
  }

  process.on('SIGINT', function() {
    if(process.stdin.isTTY) {
      exit()
    } else {
      setInterval(() => {
        if(process.stdin.readableLength === 0) {
          exit()
        }
      }, 10)
    }
  })
}

function tryToLoadService(protobufs, service) {
  try {
    const messages =  protobuf.loadFromPaths([
      protobuf.getGoogleSchemasPath(),
      protobuf.getMSGSchemasPath(),
      ...protobufs,
    ])
    const index = protobuf.makeFlatIndex(messages)
    return index.services[service]
  } catch(ex) {
    process.stderr.write(`${ex}\n`)
    return null
  }
}
