const fsLib = require('fs')
const pathLib = require('path')
const protobuf = require('protobufjs')
const grpc = require('@grpc/grpc-js')

function loadDirectory(directoryPath) {
  const messages = new protobuf.Root()
  // TODO: use custom resolver to load google protobufs from here rather than requiring them
  // TODO: recurse into subdirectories
  /* Code I kept for this ^ \/\/\/
  if(fsLib.statSync(filepath).isDirectory()) {
    return changeDirectory(filepath)
  }
  */
  fsLib
    .readdirSync(directoryPath)
    .filter(filename=>filename.endsWith('.proto'))
    .forEach(filename=> {
      const filePath = pathLib.join(directoryPath, filename)
      messages.loadSync(filePath,{
            keepCase: true,
            enums: String,
            defaults: true,
            oneofs: true,
          });
    });

  // Important - ensures that fields like resolvedType contain a value
  messages.resolveAll()

  return messages
}

function createDeserializer(msg) {
  return buf => msg.toObject(msg.decode(buf))
}

function createSerializer(msg) {
  return obj => msg.encode(msg.fromObject(obj)).finish()
}
function enhanceService(service, name) {
  // grpc-js needs some extra stuff to do its job
  Object.keys(service.methods).forEach(methodKey => {
    const method = service.methods[methodKey];
    method.path = '/' + name + '/' + method.name
    method.requestStream = !!method.requestStream
    method.responseStream = !!method.responseStream
    method.requestSerialize = createSerializer(method.resolvedRequestType)
    method.requestDeserialize = createDeserializer(method.resolvedRequestType)
    method.responseSerialize = createSerializer(method.resolvedResponseType)
    method.responseDeserialize = createDeserializer(method.resolvedResponseType)
    method.originalName = method.name
  })
}

function makeFlatIndex(node, ns) {
  const namespace = ns || []
  const index = {
    messages: {},
    services: {},
    clients: {},
  };

  switch(node.constructor.name) {
    case 'Type': // a message
      index.messages[namespace.join('.')] = node
      break
    case 'Service':
      enhanceService(node, namespace.slice(-1))
      index.services[namespace.join('.')] = node
      index.clients[namespace.join('.')] = grpc.makeClientConstructor(node.methods, namespace.slice(-1), {})
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
      index.clients = Object.assign(index.clients, ...Object.keys(childIndex.clients).map(k => ({ [k]: childIndex.clients[k] })))
    })
  }

  return index
}

function describeServiceMethods(service, client) {
  return Object.assign(...Object.keys(service.methods).map(serviceMethodKey => {
    const serviceMethod = service.methods[serviceMethodKey];
    return {
      [serviceMethodKey]: {
        method: service.originalName,
        requestType: serviceMethod.requestType,
        requestSample: makeFullySpecifiedJsonSample(serviceMethod.resolvedRequestType),
        responseOf: serviceMethod.responseStream ? "stream of" : "",
        responseType:  serviceMethod.responseType,
        responseSample: makeFullySpecifiedJsonSample(serviceMethod.resolvedResponseType),
        invokeRpc: (host, port, request) => { // here's a function which will call the service! yay
          const clientImpl = new client(`${host}:${port}`, grpc.credentials.createInsecure())
          return new Promise((resolve,reject) => {
            if(serviceMethod.responseStream) {
              const call = clientImpl[serviceMethodKey](request)
              const buffer = []
              call.on('data', d => buffer.push(d))
              call.on('end', () => resolve(buffer))
              call.on('error', err => reject(err))
            } else {
              clientImpl[serviceMethodKey](request, (err, response) => {
                if(err) {
                  reject(err)
                } else {
                  resolve(response)
                }
              });
            }
          })
        }
      }
    }
  }))
}

function makeFullySpecifiedJsonSample(messageType) {
  switch(messageType.constructor.name) {
    case 'Enum':
      return Object.keys(messageType.values).join("|")
    case 'Type':
    default: // assume anything we don't understand is a message and hope for the best
      if(!messageType.fields || messageType.fields.length === 0) {
        return {};
      }
      return Object.assign(...Object.keys(messageType.fields).map(fieldKey => {
        const field = messageType.fields[fieldKey];

        const wrap = field.repeated ?
          (val => { return { [field.name]: [ val ] } }) :
          (val => { return { [field.name]: val } })

        if(field.resolvedType) {
          // console.log(messageType.name, field.name, field.resolvedType);
          return wrap(makeFullySpecifiedJsonSample(field.resolvedType))
        }

        switch(field.name) {
          case "bool":
            return wrap(false)
          case "string":
            return wrap(`${field.name} ${Math.ceil(1000*Math.random())}`)
          case "bytes":
            return wrap(btoa(`${field.name} ${Math.ceil(1000*Math.random())}`))
          default:
            // everything else is a number :)
            // console.log(messageType.name, field.name, field);
            return wrap(Math.ceil(1000*Math.random()))
        }
      }))
  }
}

module.exports = {
  loadDirectory,
  makeFlatIndex,
  describeServiceMethods,
  makeFullySpecifiedJsonSample,
};
