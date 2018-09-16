const protobuf = require('../protobuf')(require('protobufjs'))
const { SchemaConverter } = require('../protobuf.convert')
const { readUTF8Lines } = require('../streams')
const { matchesFilter } = require('./filter')
const { inspect } = require('util')

module.exports = {
  decode,
}

function decode(schemaName, encodingName, delimiterBuffer, protobufPath, filterJsonObject, templateFunction) {
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)
  const converter = new SchemaConverter(schema)

  const lineStream = readUTF8Lines(process.stdin)
  const serialiseJsonObject = process.stdout.isTTY ? x=>inspect(x, {
    colors: true,
    depth: null,
  }) : x=>JSON.stringify(x)

  lineStream.on('line', line => {
    const jsonObject = converter.string_encoded_binary_to_json_object(line, encodingName)
    if(matchesFilter(jsonObject, filterJsonObject)) {
      process.stdout.write(serialiseJsonObject(templateFunction(jsonObject)))
      process.stdout.write(delimiterBuffer)
    }
  })

  if(process.stdin.isTTY) {
    console.log(`Start typing ${schemaName} lines as ${encodingName}`)
  }

  lineStream.on('end', () => { process.exit() })
}