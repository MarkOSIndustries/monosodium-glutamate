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
        const suggestionContext = getSuggestionContext(model, position)
        return suggest(state.method.requestType, [], monaco, suggestionContext)
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

function suggest(messageType, fieldNameChain, monaco, suggestionContext) {
  switch(messageType.constructor.name) {
    case 'Enum':
      if(suggestionContext.values && isSameFieldNameChain(fieldNameChain, suggestionContext.fieldNameChain)) {
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
        const nextFieldNameChain = fieldNameChain.concat(fieldKey)
        let fieldType = messageType.fields[fieldKey].type
        if(messageType.fields[fieldKey].resolvedType) {
          fieldType = messageType.fields[fieldKey].resolvedType.name
          result.push(...suggest(messageType.fields[fieldKey].resolvedType, nextFieldNameChain, monaco, suggestionContext))
        }
        if(suggestionContext.fields && isSameFieldNameChain(fieldNameChain, suggestionContext.fieldNameChain)) {
          result.push({
            label: JSON.stringify(fieldKey),
            kind: monaco.languages.CompletionItemKind.Field,
            detail: fieldType,
            documentation: `${nextFieldNameChain.join('.')} = ${messageType.fields[fieldKey].id};`,
          })
        }
        if(suggestionContext.values && isSameFieldNameChain(nextFieldNameChain, suggestionContext.fieldNameChain)) {
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

  const values = (lastColon > lastComma && lastColon > lastOpenBracket)
  const fields = !values

  return {
    values,
    fields,
    fieldNameChain: buildFieldNameChain(textPrecedingCursor),
  }
}

function buildFieldNameChain(textPrecedingCursor) {
  const fieldNames = []
  let levelComplete = false
  let closeBracketCount = 0
  for(let index = textPrecedingCursor.length-1; index >= 0; index--) {
    if(textPrecedingCursor[index] === ',') {
      levelComplete = true
    }
    if(textPrecedingCursor[index] === '}') {
      closeBracketCount = closeBracketCount + 1
      levelComplete = true
    }
    if(textPrecedingCursor[index] === '{') {
      closeBracketCount = closeBracketCount - 1
      if(closeBracketCount < 0) {
        levelComplete = false
        closeBracketCount = 0
      }
    }
    if(!levelComplete && textPrecedingCursor[index] === ':') {
      const fieldName = getLastQuotedChunk(textPrecedingCursor.substring(0,index))
      fieldNames.unshift(fieldName)
      levelComplete = true
    }
  }
  return fieldNames
}

function getLastQuotedChunk(str) {
  const chunks = str.split('"')
  return chunks[chunks.length-2]
}

function isSameFieldNameChain(chain1, chain2) {
  return chain1.join('.') === chain2.join('.')
}
