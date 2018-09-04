// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const pathLib = require('path')
const protobuf = require('../protobuf.js')
const kafka = require('../kafka.js')

const dom = {
  schemaSearch: document.querySelector('#schema-search'),
  schemaListing: document.querySelector('#schema-listing'),
  schemaDetails: document.querySelector('#schema-details'),
  queryEditor: document.querySelector(`#query-editor`),
  loadTopics: document.querySelector(`#load-topics`),
  topicListing: document.querySelector(`#topic-listing`),
  streamListing: document.querySelector('#stream-listing'),
  brokerHost: document.querySelector('#broker-host'),
  brokerPort: document.querySelector('#broker-port'),
  brokerStatus: document.querySelector('#broker-status'),
  brokerListing: document.querySelector('#broker-listing'),
  flowStartTime: document.querySelector('#flow-start-time'),
  flowEndTime: document.querySelector('#flow-end-time'),
  flowMaxMessages: document.querySelector('#flow-max-messages'),
  flowStart: document.querySelector('#flow-start'),
  flowStop: document.querySelector('#flow-stop'),
}

const now = Date.now()
dom.flowStartTime.valueAsNumber = (now - now%(1000*60*60*24)) // start of today UTC
dom.flowEndTime.valueAsNumber = (now - now%(60*1000)) // start of this minute

const selected = {
  topic: null,
  schema: null,
  schemaName: '',
}

const globals = {
  queryEditor: {},
}

require('monaco-loader')().then(monaco => {
  globals.queryEditor = monaco.editor.create(dom.queryEditor, {
    value: '{}',
    language: 'json',
    theme: 'vs-light',
    automaticLayout: true,
    scrollBeyondLastLine: false,
    minimap: {
      enabled: false
    }
  })

  if(localStorage.getItem('last-selected-directory')) {
    changeDirectory(localStorage.getItem('last-selected-directory'))
  }
})

function changeDirectory(path) {
  document.title = `Kafka Protobuf GUI - ${path}`
  const messages = protobuf.loadDirectory(path)

  const messagesIndex = protobuf.makeFlatIndex(messages)

  const domEntryList = document.createElement('ul')
  domEntryList.className = 'entry-list'

  dom.schemaListing.innerHTML = ''
  dom.schemaListing.appendChild(domEntryList)

  Object.keys(messagesIndex.messages).forEach(fqSchemaName => {
    const schema = messagesIndex.messages[fqSchemaName]
    const schemaId = fqSchemaName.replace(/\./gi, '-')
    const domEntry = document.createElement('li')
    domEntry.className = 'schema-listing-entry clickable-entry tab'
    domEntry.setAttribute('data-fq-schema-name', fqSchemaName)

    domEntry.innerHTML =
      `<div class="layout layout-row" style="justify-content:space-between;padding-right:0.2em">`+
        `<h6 style="float:right"><pre>${schema.filename}</pre></h6>`+ // TODO: don't float?
        `<h5><code>${fqSchemaName}</code></h5>`+
      `</div>`

    domEntry.addEventListener('click', clickEvent => {
      const lastSelectedSchemaDomElement = document.querySelector('#schema-listing .schema-listing-entry.selected')
      if(lastSelectedSchemaDomElement) {
        lastSelectedSchemaDomElement.classList.remove('selected')
      }
      domEntry.classList.add('selected')

      selected.schema = schema
      selected.schemaName = fqSchemaName

      globals.queryEditor.setValue(localStorage.getItem(`${selected.schemaName}-query`) || '{}')
      Object.values(document
        .querySelectorAll('#topic-listing select option')).filter(option => option.value.toLowerCase().indexOf(fqSchemaName.toLowerCase()) > -1)
        .forEach(x => { console.log('Auto selecting topic', x); x.selected = true })
    })

    domEntryList.appendChild(domEntry)
  })

  nextSchema()
  globals.queryEditor.focus()
}

async function refreshTopics() {
  try {
    const kafkaClient = await kafka.getKafkaClient([{host: dom.brokerHost.value, port: dom.brokerPort.value}]) // TODO: make this happen only when fields change
    const topics = Object.keys(kafkaClient.topicMetadata)
    kafkaClient.close()

    dom.brokerListing.innerHTML = `from ${dom.brokerHost.value}:${dom.brokerPort.value}`
    dom.topicListing.innerHTML = '<select id="topic-select">'+
      topics.map(topic => {
        return `<option value="${topic}">${topic}</option>`
      }).join('')+
    '</select>'
  } catch(ex) {
    console.warn('Could not get topics from broker', dom.brokerHost.value, dom.brokerPort.value, ex)
  }
}

dom.loadTopics.addEventListener('click', refreshTopics)
dom.brokerHost.addEventListener('blur', refreshTopics)
dom.brokerPort.addEventListener('blur', refreshTopics)
refreshTopics()

async function runQuery() {
  try {
    const topics = [ document.querySelector('#topic-select').value ]
    const tableBody = dom.streamListing.querySelector('tbody')

    const kafkaClient = await kafka.getKafkaClient([{host: dom.brokerHost.value, port: dom.brokerPort.value}]) // TODO: make this happen only when fields change
    console.log('got client')
    const topicPartitionOffsets = await kafka.getOffsetsAtTime(kafkaClient, topics, dom.flowStartTime.valueAsNumber)
    console.log('got offsets', topicPartitionOffsets, dom.flowStartTime.valueAsNumber)
    const maxMessages = dom.flowMaxMessages.value
    const messageRows = []
    const domUpdateInterval = setTimeout(() => {
      tableBody.innerHTML = ''
      // TODO: measure perf against a high volume topic - might be worth trying to insert in order...
      messageRows.sort((r1,r2) => r1.ts-r2.ts).forEach(row => tableBody.appendChild(row.domRow))
    }, 100)
    await kafka.consumeFromOffsets(kafkaClient, topicPartitionOffsets, (message) => {
      console.log(message.timestamp.getTime(), dom.flowEndTime.valueAsNumber)
      if(message.timestamp.getTime() < dom.flowEndTime.valueAsNumber && message.timestamp.getTime() >= dom.flowStartTime.valueAsNumber) {
        const domRow = document.createElement('tr')
        let decoded
        try {
          decoded = selected.schema.decode(message.value)
        } catch(ex) {
          decoded = {
            _error: ex.toString(),
            _schema: selected.schemaName,
            _message: message.value.toString('hex'),
          }
        }

        domRow.innerHTML =
        `<td>${message.partition}</td>`+
        `<td>${message.offset}</td>`+
        `<td>${message.timestamp.toISOString()}</td>`+
        `<td>${message.key}</td>`+
        `<td><pre>${JSON.stringify(decoded, undefined, '  ')}</pre></td>`

        messageRows.push({ts: message.timestamp.getTime(), domRow })
      }

      return (messageRows.length < maxMessages)
    })
    console.log('clearing')
    clearInterval(domUpdateInterval)
    kafkaClient.close()
  } catch(ex) {
    console.error(`Couldn't run query`, ex)
  }
}

dom.flowStart.addEventListener('click', runQuery)

function searchUpdated() {
  document.querySelectorAll('#schema-listing .schema-listing-entry').forEach(schemaDomElement => {
    const fqSchemaName = schemaDomElement.attributes["data-fq-schema-name"].value
    const searchRegex = new RegExp(dom.schemaSearch.value, "i")
    if(dom.schemaSearch.value == '' || -1 < fqSchemaName.search(searchRegex)) {
      schemaDomElement.classList.remove('hidden')
    } else {
      schemaDomElement.classList.add('hidden')
    }
  })
}

dom.schemaSearch.addEventListener('keydown', event => {
  var key = event.which || event.keyCode;
  switch(key) {
    case 13: // Enter
    case 27: // Esc
      dom.schemaSearch.value = ''
      globals.queryEditor.focus()
      return event.preventDefault()
    case 38: // Up
      previousSchema()
      return event.preventDefault()
    case 40: // Down
      nextSchema()
      return event.preventDefault()
    default:
      setTimeout(() => searchUpdated(), 0)
  }
})
dom.schemaSearch.addEventListener('blur', searchUpdated)

function previousSchema() {
  let currentSchemaDomElement = document.querySelector('#schema-listing .schema-listing-entry.selected')
  do {
    if(currentSchemaDomElement) {
      currentSchemaDomElement = currentSchemaDomElement.previousElementSibling
    }
    if(!currentSchemaDomElement) {
      currentSchemaDomElement = document.querySelector('#schema-listing .schema-listing-entry:last-child')
    }
  } while(currentSchemaDomElement && !currentSchemaDomElement.classList.contains('selected') && (currentSchemaDomElement.classList.contains('hidden') || !currentSchemaDomElement.classList.contains('schema-listing-entry')))

  if(currentSchemaDomElement) {
    currentSchemaDomElement.click()
  }
}

function nextSchema() {
  let currentSchemaDomElement = document.querySelector('#schema-listing .schema-listing-entry.selected')
  do {
    if(currentSchemaDomElement) {
      currentSchemaDomElement = currentSchemaDomElement.nextElementSibling
    }
    if(!currentSchemaDomElement) {
      currentSchemaDomElement = document.querySelector('#schema-listing .schema-listing-entry:first-child')
    }
  } while(currentSchemaDomElement && !currentSchemaDomElement.classList.contains('selected') && (currentSchemaDomElement.classList.contains('hidden') || !currentSchemaDomElement.classList.contains('schema-listing-entry')))

  if(currentSchemaDomElement) {
    currentSchemaDomElement.click()
  }
}

ipc.on('selected-directory', (event, paths) => {
  const [path] = paths
  changeDirectory(path)
  localStorage.setItem('last-selected-directory', path)
})
ipc.on('run-query', runQuery)
ipc.on('next-schema', () => {
  nextSchema()
  globals.queryEditor.focus()
})
ipc.on('previous-schema', () => {
  previousSchema()
  globals.queryEditor.focus()
})
ipc.on('find-schema', () => dom.schemaSearch.focus())
ipc.on('select-matching-topic', () => globals.selectMatchingTopic())
