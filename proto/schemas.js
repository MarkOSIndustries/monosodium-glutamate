const protobuf = require('../protobuf')(require('protobufjs'))

module.exports = {
  schemas
}

function schemas({query, protobufs}) {
  const allSchemas = protobuf.loadFromPaths([
    protobuf.getGoogleSchemasPath(),
    protobuf.getMSGSchemasPath(),
    ...protobufs,
  ])
  const index = protobuf.makeFlatIndex(allSchemas)
  Object.keys(index.messages).forEach(schemaName => {
    if(query.test(schemaName)) {
      console.log(schemaName, Object.values(index.messages[schemaName].fields).map(field => {
        return `${field.name}: ${field.repeated ? 'repeated ' : ''}${field.resolvedType ? field.resolvedType.name : field.type}`
      }))
    }
  })
}
