const protobuf = require('../protobuf')(require('protobufjs'))

module.exports = {
  services
}

function services({query, protobufs}) {
  const allSchemas = protobuf.loadFromPaths([
    protobuf.getGoogleSchemasPath(),
    protobuf.getMSGSchemasPath(),
    ...protobufs,
  ])
  const index = protobuf.makeFlatIndex(allSchemas)
  Object.keys(index.services).forEach(serviceName => {
    if(query.test(serviceName)) {
      console.log(serviceName, Object.keys(protobuf.describeServiceMethods(index.services[serviceName])))
    }
  })
}
