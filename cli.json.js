const { inspect } = require('util')

function getJsonRenderer(stdoutIsTTY) {
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

module.exports = {
  getJsonRenderer,
}
