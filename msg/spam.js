const protobuf = require('../protobuf')(require('protobufjs'))
const { matchesFilter } = require('./filter')
const { inspect } = require('util')

module.exports = {
  spam,
}

function spam(schemaName, delimiterBuffer, protobufPath, filterJsonObject) {
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)

// TODO extract this - it's in 3 files already
  const serialiseJsonObject = process.stdout.isTTY ? x=>inspect(x, {
    colors: true,
    depth: null,
  }) : x=>JSON.stringify(x)

  while(true) {
    const jsonObject = Object.assign(protobuf.makeValidJsonRecord(schema), filterJsonObject)
    process.stdout.write(serialiseJsonObject(jsonObject))
    process.stdout.write(delimiterBuffer)
  }

  process.stdin.on('end', () => { process.exit() })
}
