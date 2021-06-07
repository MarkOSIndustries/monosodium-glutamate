const { getJsonRenderer } = require('../cli.json')

function coerceTemplate(templateString, ttyString) {
  const stdoutIsTTY = coerceTTY(ttyString)
  if(templateString == null) {
    return getJsonRenderer(stdoutIsTTY)
  }

  // Whatever the user asked for - providing the current json message as 'msg'
  const escapedTemplateString = '`'+templateString.replace(/`/,'\\`')+'`'
  return (msg) => eval(escapedTemplateString)
}

function coerceTTY(ttyString) {
  switch(ttyString) {
    case 'y': return true
    case 'n': return false
    default: return Boolean(process.stdout.isTTY)
  }
}

module.exports = {
  coerceTemplate,
  coerceTTY,
}
