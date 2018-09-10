const protobuf = require('../protobuf')(require('protobufjs'))
const { SchemaConverter } = require('../protobuf.convert')
const { readUTF8Lines, writeLengthPrefixedBuffers } = require('../streams')
const { matchesFilter } = require('./filter')

module.exports = {
  serialise,
}

function serialise(schemaName, prefixFormat, protobufPath, filterJsonObject) {
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)
  const converter = new SchemaConverter(schema)

  const lineStream = readUTF8Lines(process.stdin)
  const prefixedBinaryStream = writeLengthPrefixedBuffers(process.stdout, prefixFormat)

  lineStream.on('line', line => {
    const jsonObject = JSON.parse(line)
    if(matchesFilter(jsonObject, filterJsonObject)) {
      const binaryBuffer = converter.json_object_to_binary_buffer(jsonObject)
      prefixedBinaryStream.write(binaryBuffer)
    }
  })

  if(process.stdin.isTTY) {
    console.log(`Start typing ${schemaName} lines as JSON`)
  }

  lineStream.on('end', () => { process.exit() })
}
