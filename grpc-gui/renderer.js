// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const pathLib = require('path')
const protobuf = require('../protobuf')(require('protobufjs'))
const rxmq = require('rxmq').default

const channels = {
  services: rxmq.channel("services"),
  request: rxmq.channel("request"),
  invocation: rxmq.channel("invocation"),
  response: rxmq.channel("response"),
}

const services = require('./services')(channels)
const request = require('./request')(channels)
const invocation = require('./invocation')(channels)
const response = require('./response')(channels)
const history = require('./history')(channels)

const workspace = {
  paths: [],
}

function newWorkspace() {
  workspace.paths = []
  addPaths([protobuf.getGoogleSchemasPath()])
}

function addPaths(paths) {
  workspace.paths.push(...paths)
  workspace.paths = Array.from(new Set(workspace.paths))
  document.title = `GRPC GUI - ${workspace.paths.join(', ')}`

  const messages = protobuf.loadFromPaths(workspace.paths)
  const messagesIndex = protobuf.makeFlatIndex(messages)
  const methods = []
  Object.keys(messagesIndex.services).forEach(serviceName => {
    methods.push(...Object.values(protobuf.describeServiceMethods(messagesIndex.services[serviceName], serviceName)))
  })

  services.load(methods)

  localStorage.setItem('last-workspace-paths', workspace.paths.join(','))
}

if(localStorage.getItem('last-workspace-paths')) {
  addPaths(localStorage.getItem('last-workspace-paths').split(','))
} else {
  newWorkspace()
}

ipc.on('new-workspace', newWorkspace)
ipc.on('added-workspace-paths', (event, paths) => addPaths(paths))
ipc.on('invoke-service-method', invocation.invoke)
ipc.on('cancel-service-method', invocation.cancel)
ipc.on('next-service-method', services.nextMethod)
ipc.on('previous-service-method', services.previousMethod)
ipc.on('find-service-method', services.search)
ipc.on('generate-request-body', request.loadSample)
ipc.on('load-last-request-body', request.loadLastSent)
