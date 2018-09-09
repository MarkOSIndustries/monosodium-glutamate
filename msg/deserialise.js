const protobuf = require('../protobuf')(require('protobufjs'))
const { SchemaConverter } = require('../protobuf.convert')
const { readLengthPrefixedBuffers } = require('../streams')
const {inspect} = require('util')

module.exports = {
  deserialise,
}

function deserialise(schemaName, prefixFormat, delimiterBuffer, protobufPath) {
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)
  const converter = new SchemaConverter(schema)

  const prefixedBinaryStream = readLengthPrefixedBuffers(process.stdin, prefixFormat)
  const serialiseJsonObject = process.stdout.isTTY ? x=>inspect(x, {
    colors: true,
    depth: null,
  }) : x=>JSON.stringify(x)

  prefixedBinaryStream.on('data', binaryBuffer => {
    const jsonObject = converter.binary_buffer_to_json_object(binaryBuffer)
    process.stdout.write(serialiseJsonObject(jsonObject))
    process.stdout.write(delimiterBuffer)
  })

  if(process.stdin.isTTY) {
    console.log(`This mode doesn't really support interactive. Try piping input...`)
    process.exit()
  }

  prefixedBinaryStream.on('end', () => { process.exit() })
}
