const fsLib = require('fs')
const pathLib = require('path')
const {rng} = require('crypto')

function initWithProtobufJS(protobufjs) {
  function loadFromPaths(directoryPaths, messages) {
    const shouldResolve = !messages
    messages = messages || new protobufjs.Root()
    if(!Array.isArray(directoryPaths)) {
      directoryPaths = [directoryPaths]
    }

    directoryPaths.forEach(directoryPath => {
      fsLib
        .readdirSync(directoryPath)
        .forEach(filename => {
          const filePath = pathLib.join(directoryPath, filename)
          if(fsLib.statSync(filePath).isDirectory()) {
            loadFromPaths(filePath, messages)
            return
          }

          if(filename.endsWith('.proto')) {
            try {
              messages.loadSync(filePath,{
                    keepCase: true,
                    enums: String,
                    defaults: true,
                    oneofs: true,
                  })

            } catch(ex) {
              if(!ex.message.startsWith('duplicate name')) {
                console.error(filePath, '::', ex.message)
              }
            }
          }
        })
    })

    // Important - ensures that fields like resolvedType contain a value
    if(shouldResolve) {
      messages.resolveAll()
    }

    return messages
  }

  function getGoogleSchemasPath() {
    return pathLib.join(__dirname, 'schemas', 'google')
  }

  function getMSGSchemasPath() {
    return pathLib.join(__dirname, 'schemas', 'src', 'main', 'proto')
  }

  function makeFlatIndex(node, ns) {
    const namespace = ns || []
    const index = {
      messages: {},
      services: {},
    };

    switch(node.constructor.name) {
      case 'Type': // a message
        index.messages[namespace.join('.')] = node
        break
      case 'Service':
        index.services[namespace.join('.')] = node
        break

      // Unused but kept for quick reference
      case 'Namespace':
      case 'Field':
      case 'Object': // probably an enum
        break
    }

    if(node.hasOwnProperty('nested') && node.nested !== undefined) {
      Object.keys(node.nested).map(key => makeFlatIndex(node.nested[key], [...namespace, key])).forEach(childIndex => {
        index.messages = Object.assign(index.messages, ...Object.keys(childIndex.messages).map(k => ({ [k]: childIndex.messages[k] })))
        index.services = Object.assign(index.services, ...Object.keys(childIndex.services).map(k => ({ [k]: childIndex.services[k] })))
      })
    }

    return index
  }

  function describeServiceMethods(service, serviceName) {
    return Object.assign({}, ...Object.keys(service.methods).map(serviceMethodKey => {
      const serviceMethod = service.methods[serviceMethodKey];
      return {
        [serviceMethodKey]: {
          fqName: `${serviceName}.${serviceMethodKey}`,
          serviceName,
          methodName: serviceMethodKey,
          requestOf: serviceMethod.requestStream ? "stream of" : "",
          requestType: serviceMethod.resolvedRequestType,
          requestTypeName: serviceMethod.requestType,
          requestSample: makeValidJsonRecord(serviceMethod.resolvedRequestType),
          responseOf: serviceMethod.responseStream ? "stream of" : "",
          responseType: serviceMethod.resolvedResponseType,
          responseTypeName: serviceMethod.responseType,
          responseSample: makeValidJsonRecord(serviceMethod.resolvedResponseType),
          invokeWith: (http2Connection, requestObject, options) => {
            const {requestsStream, responsesStream} = http2Connection.rpcStreams(service, serviceMethod, options)
            requestsStream.write(requestObject)
            requestsStream.end()
            return responsesStream
          },
          makeRpcStreams: (http2Connection, options) => {
            return http2Connection.rpcStreams(service, serviceMethod, options)
          }
        }
      }
    }))
  }

  function makeValidJsonRecord(messageType, messageTypeBlacklist = []) {
    switch(messageType.constructor.name) {
      case 'Enum':
        const keys = Object.keys(messageType.values)
        return keys[rng(2).readUInt16BE()%keys.length]
      case 'Type':
      default: // assume anything we don't understand is a message and hope for the best
        if(!messageType.fields) {
          return {}
        }
        return Object.assign({}, ...Object.keys(messageType.fields).map(fieldKey => {
          const field = messageType.fields[fieldKey]

          const wrap =
            field.keyType ?
              (val => { return { [field.name]: { [makeTypeSample(field.keyType, `${field.name}_key`)]: val } } }) :
            field.repeated ?
              (val => { return { [field.name]: [ val ] } }) :
              (val => { return { [field.name]: val } })

          if(field.resolvedType) {
            // Prevent recursing to generate a type we saw further up the chain
            if(new Set(messageTypeBlacklist).has(field.resolvedType)) {
              return {}
            }
            return wrap(makeValidJsonRecord(field.resolvedType, [...messageTypeBlacklist, messageType]))
          }

          return wrap(makeTypeSample(field.type, field.name))
        }))
    }
  }

  function makeTypeSample(fieldType, fieldName) {
    switch(fieldType) {
      case "bool":
        return Boolean(rng(1).readUIntBE(0, 1)%2)
      case "string":
        return `${fieldName} ${rng(2).readUInt16BE(0, 2)}`
      case "bytes":
        return Buffer.concat([Buffer.from(fieldName), rng(12)]).toString("base64")
      default:
        // everything else is a number :)
        return rng(2).readUInt16BE(0,2)
    }
  }

  return {
    loadFromPaths,
    getGoogleSchemasPath,
    getMSGSchemasPath,
    makeFlatIndex,
    describeServiceMethods,
    makeValidJsonRecord,
    makeTypeSample,
  }
}

module.exports = initWithProtobufJS
