const protobuf = require('../protobuf')(require('protobufjs'))

module.exports = {
  schemas
}

function schemas({query, protobufs}) {
  const allSchemas = protobuf.loadDirectory(protobufs)
  const index = protobuf.makeFlatIndex(allSchemas)
  Object.keys(index.messages).forEach(schemaName => {
    if(query.test(schemaName)) {
      console.log(schemaName, Object.keys(index.messages[schemaName].fields))
    }
  })
}
