const protobuf = require('../protobuf')(require('protobufjs'))

module.exports = function(channels) {
  const cmd = {
    loadSample: () => {
      if(!state.method) return false
      channels.request.subject('payload.replaced').next(JSON.stringify(state.method.requestSample, undefined, '  '))
      return true
    },
    loadLastSent: () => {
      if(!state.method) return false
      const lastSent = localStorage.getItem(`${state.method.serviceName}-${state.method.methodName}-request`)
      if(!lastSent) return false
      channels.request.subject('payload.replaced').next(lastSent)
      return true
    },
  }

  const state = {
    method: null,
  }

  const requestEditorPromise = require('monaco-loader')().then(monaco => {
    monaco.languages.registerCompletionItemProvider('json', {
      provideCompletionItems: function (model, position) {
        return suggest(state.method.requestType, state.method.requestTypeName, monaco, model, position)
      }
    })

    return monaco.editor.create(document.querySelector(`.request-json`), {
      value: '{}',
      language: 'json',
      theme: 'vs-light',
      automaticLayout: true,
      scrollBeyondLastLine: false,
      minimap: {
        enabled: false
      }
    })
  })

  channels.request.subject('payload.replaced').subscribe(payload => {
    requestEditorPromise.then(requestEditor => {
      requestEditor.setValue(payload)
      channels.request.subject('payload.changed').next(payload)
    })
  })

  requestEditorPromise.then(requestEditor => requestEditor.onDidChangeModelContent((e) => {
    channels.request.subject('payload.changed').next(requestEditor.getValue())
  }))

  channels.services.subject('method.selection.finished').subscribe(() => {
    requestEditorPromise.then(requestEditor => requestEditor.focus())
  })

  channels.services.subject('method.selection.changed').subscribe(method => {
    state.method = method
    if(!cmd.loadLastSent()) cmd.loadSample()
  })

  channels.invocation.subject('started').subscribe(requestPayload => {
    localStorage.setItem(`${state.method.serviceName}-${state.method.methodName}-request`, requestPayload)
  })

  return cmd
}

function suggest(messageType, fieldName, monaco, monacoModel, monacoPosition) {
  const suggestionContext = getSuggestionContext(monacoModel, monacoPosition)

  switch(messageType.constructor.name) {
    case 'Enum':
      if(suggestionContext.values && fieldName == suggestionContext.fieldName) {
        return Object.keys(messageType.values).map(enumValue => {
          return {
            label: JSON.stringify(enumValue),
            kind: monaco.languages.CompletionItemKind.Enum,
            detail: messageType.name,
            documentation: enumValue,
          }
        })
      } else {
        return []
      }
    case 'Type':
    default: // assume anything we don't understand is a message and hope for the best
      const result = []
      if(!messageType.fields) return result
      Object.keys(messageType.fields).forEach(fieldKey => {
        let fieldType = messageType.fields[fieldKey].type
        if(messageType.fields[fieldKey].resolvedType) {
          fieldType = messageType.fields[fieldKey].resolvedType.name
          result.push(...suggest(messageType.fields[fieldKey].resolvedType, fieldKey, monaco, monacoModel, monacoPosition))
        }
        if(suggestionContext.fields) {
          result.push({
            label: JSON.stringify(fieldKey),
            kind: monaco.languages.CompletionItemKind.Field,
            detail: fieldType,
            documentation: `${messageType.fields[fieldKey].toString()} = ${messageType.fields[fieldKey].id}`,
          })
        }
        if(suggestionContext.values && fieldKey == suggestionContext.fieldName) {
          let defaultValue = messageType.fields[fieldKey].typeDefault
          if(!defaultValue) {
            defaultValue = JSON.stringify(defaultValue)
          } else if(Array.isArray(messageType.fields[fieldKey].typeDefault)) {
            // bytes default to empty array for some reason
            defaultValue = JSON.stringify("")
          } else {
            // Needed to handle longs, which are represented as objects
            defaultValue = defaultValue.toString()
          }
          result.push({
            label: defaultValue,
            kind: monaco.languages.CompletionItemKind.Value,
            detail: messageType.fields[fieldKey].type,
            documentation: 'Default value',
          })
          if(!messageType.fields[fieldKey].resolvedType) {
            if(fieldType === 'bool') {
              result.push({
                label: "true",
                kind: monaco.languages.CompletionItemKind.Value,
                detail: messageType.fields[fieldKey].type,
              })
            } else {
              result.push({
                label: JSON.stringify(protobuf.makeTypeSample(messageType.fields[fieldKey].type, fieldKey)),
                kind: monaco.languages.CompletionItemKind.Value,
                detail: messageType.fields[fieldKey].type,
                documentation: 'Randomly generated value',
              })
            }
          }
        }
      })
      return result
  }
}

function getSuggestionContext(monacoModel, monacoPosition) {
  const textPrecedingCursor = monacoModel.getValueInRange({startLineNumber: 1, startColumn: 1, endLineNumber: monacoPosition.lineNumber, endColumn: monacoPosition.column})
  const lastColon = textPrecedingCursor.lastIndexOf(':')
  const lastOpenBracket = textPrecedingCursor.lastIndexOf('{')
  const lastComma = textPrecedingCursor.lastIndexOf(',')

  if(lastColon > lastComma && lastColon > lastOpenBracket) {
    // We're suggesting a value
    const chunks = textPrecedingCursor.split(':')
    const fieldNameChunk = chunks[chunks.length-2]
    const valueChunks = fieldNameChunk.split('"')
    const fieldName = valueChunks[valueChunks.length-2]
    return {
      values: true,
      fieldName,
    }
  }

  // we're suggesting a field
  return {
    fields: true,
  }
}
