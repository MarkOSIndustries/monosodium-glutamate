const protobuf = require('../protobuf')(require('protobufjs'))
const { inspect } = require('util')

module.exports = {
  spam,
}

function spam(schemaName, delimiterBuffer, protobufPath) {
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)

// TODO extract this - it's in 3 files already
  const serialiseJsonObject = process.stdout.isTTY ? x=>inspect(x, {
    colors: true,
    depth: null,
  }) : x=>JSON.stringify(x)

  while(true) {
    const jsonObject = protobuf.makeValidJsonRecord(schema) // TODO: add a version which generates VALID payloads (currently it includes all enum values)
    process.stdout.write(serialiseJsonObject(jsonObject))
    process.stdout.write(delimiterBuffer)
  }

  process.stdin.on('end', () => { process.exit() })
}
