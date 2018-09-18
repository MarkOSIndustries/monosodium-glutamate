const fsLib = require('fs')
const pathLib = require('path')
const {rng} = require('crypto')

function initWithProtobufJS(protobufjs) {
  function populate(messages, directoryPaths) {
    if(!Array.isArray(directoryPaths)) {
      directoryPaths = [directoryPaths]
    }

    directoryPaths.forEach(directoryPath => {
      fsLib
        .readdirSync(directoryPath)
        .forEach(filename => {
          const filePath = pathLib.join(directoryPath, filename)
          if(fsLib.statSync(filePath).isDirectory()) {
            populate(messages, filePath)
            return
          }

          if(filename.endsWith('.proto')) {
            messages.loadSync(filePath,{
                  keepCase: true,
                  enums: String,
                  defaults: true,
                  oneofs: true,
                })
          }
        })
    })
    return messages
  }

  function loadDirectory(...directoryPaths) {
    const messages = populate(new protobufjs.Root(), [
      pathLib.join(__dirname, 'schemas', 'google'),
      pathLib.join(__dirname, 'schemas', 'src', 'main', 'proto'),
      ...directoryPaths
    ])

    // Important - ensures that fields like resolvedType contain a value
    messages.resolveAll()

    return messages
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

  function describeServiceMethods(service) {
    return Object.assign({}, ...Object.keys(service.methods).map(serviceMethodKey => {
      const serviceMethod = service.methods[serviceMethodKey];
      return {
        [serviceMethodKey]: {
          method: serviceMethodKey,
          requestType: serviceMethod.resolvedRequestType,
          requestTypeName: serviceMethod.requestType,
          requestSample: makeFullySpecifiedJsonSample(serviceMethod.resolvedRequestType),
          responseOf: serviceMethod.responseStream ? "stream of" : "",
          responseType: serviceMethod.resolvedResponseType,
          responseTypeName: serviceMethod.responseType,
          responseSample: makeFullySpecifiedJsonSample(serviceMethod.resolvedResponseType),
          invokeWith: (http2Connection, requestObject, options) => {
            const svc = service.create(http2Connection.rpcImpl(service, options))
            const svcMethodKey = serviceMethod.name.replace(/^(.)/, c => c.toLowerCase())
            svc[svcMethodKey](requestObject)
            return svc // svc implements stream.Readable
          }
        }
      }
    }))
  }

  // TODO: deprecate in favour of makeValidJsonRecord once enum code completion is done in GRPC GUI
  function makeFullySpecifiedJsonSample(messageType) {
    switch(messageType.constructor.name) {
      case 'Enum':
        return Object.keys(messageType.values).join("|")
      case 'Type':
      default: // assume anything we don't understand is a message and hope for the best
        if(!messageType.fields || messageType.fields.length === 0) {
          return {};
        }
        return Object.assign({}, ...Object.keys(messageType.fields).map(fieldKey => {
          const field = messageType.fields[fieldKey];

          const wrap =
            field.keyType ?
              (val => { return { [field.name]: { [makeTypeSample(field.keyType, `${field.name}_key`)]: val } } }) :
            field.repeated ?
              (val => { return { [field.name]: [ val ] } }) :
              (val => { return { [field.name]: val } })

          if(field.resolvedType) {
            return wrap(makeFullySpecifiedJsonSample(field.resolvedType))
          }

          return wrap(makeTypeSample(field.type, field.name))
        }))
    }
  }

  function makeValidJsonRecord(messageType) {
    switch(messageType.constructor.name) {
      case 'Enum':
        const keys = Object.keys(messageType.values)
        return keys[rng(2).readUInt16BE()%keys.length]
      case 'Type':
      default: // assume anything we don't understand is a message and hope for the best
        if(!messageType.fields || messageType.fields.length === 0) {
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
            return wrap(makeValidJsonRecord(field.resolvedType))
          }

          return wrap(makeTypeSample(field.type, field.name))
        }))
    }
  }


  function makeTypeSample(fieldType, fieldName) {
    switch(fieldType) {
      case "bool":
        return Boolean(rng(1).readUIntBE()%2)
      case "string":
        return `${fieldName} ${rng(2).readUInt16BE()}`
      case "bytes":
        return Buffer.concat([Buffer.from(fieldName), rng(12)]).toString("base64")
      default:
        // everything else is a number :)
        return rng(2).readUInt16BE()
    }
  }

  return {
    loadDirectory,
    makeFlatIndex,
    describeServiceMethods,
    makeFullySpecifiedJsonSample,
    makeValidJsonRecord,
  }
}

module.exports = initWithProtobufJS
