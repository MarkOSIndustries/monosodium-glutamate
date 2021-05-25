const { inspect } = require('util')

function coerceTemplate(templateString, stdoutIsTTY) {
  if(templateString == null) {
    if(stdoutIsTTY) {
      // Coloured, multi-line JSON strings
      return (x) => inspect(x, {
        colors: true,
        depth: null,
      })
    } else {
      // UTF8, single-line JSON strings
      return (x) => JSON.stringify(x)
    }
  }

  // Whatever the user asked for - providing the current json message as 'msg'
  const escapedTemplateString = '`'+templateString.replace(/`/,'\\`')+'`'
  return (msg) => eval(escapedTemplateString)
}

module.exports = {
  coerceTemplate,
}
