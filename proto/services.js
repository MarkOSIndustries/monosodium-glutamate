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
      console.log(serviceName, Object.values(protobuf.describeServiceMethods(index.services[serviceName])).map(method => {
        return `${method.methodName}: ${method.requestOf} ${method.requestTypeName} => ${method.responseOf} ${method.responseTypeName}`
      }))
    }
  })
}
