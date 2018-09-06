const protobuf = require('../protobuf')
const { SchemaConverter } = require('../protobuf.convert')
const { readLengthPrefixedBuffers } = require('../streams')

module.exports = {
  deserialise,
}

function deserialise(schemaName, prefixFormat, delimiterBuffer, protobufPath) {
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)
  const converter = new SchemaConverter(schema)

  const prefixedBinaryStream = readLengthPrefixedBuffers(process.stdin, prefixFormat)

  prefixedBinaryStream.on('data', binaryBuffer => {
    const jsonObject = converter.binary_buffer_to_json_object(binaryBuffer)
    process.stdout.write(JSON.stringify(jsonObject))
    process.stdout.write(delimiterBuffer)
  })

  if(process.stdin.isTTY) {
    console.log(`This mode doesn't really support interactive. Try piping input...`)
    process.exit()
  }

  prefixedBinaryStream.on('end', () => { process.exit() })
}
