const protobuf = require('../protobuf')(require('protobufjs'))
const { SchemaConverter } = require('../protobuf.convert')
const { readUTF8Lines } = require('../streams')
const { matchesFilter } = require('./filter')

module.exports = {
  encode,
}

function encode(schemaName, encodingName, delimiterBuffer, protobufPath, filterJsonObject, templateFunction) {
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)
  const converter = new SchemaConverter(schema)

  const lineStream = readUTF8Lines(process.stdin)

  lineStream.on('line', line => {
    const jsonObject = JSON.parse(line)
    if(matchesFilter(jsonObject, filterJsonObject)) {
      const stringEncodedBinary = converter.json_object_to_string_encoded_binary(templateFunction(jsonObject), encodingName)
      process.stdout.write(stringEncodedBinary)
      process.stdout.write(delimiterBuffer)
    }
  })

  if(process.stdin.isTTY) {
    console.log(`Start typing ${schemaName} lines as JSON`)
  }

  lineStream.on('end', () => { process.exit() })
}
