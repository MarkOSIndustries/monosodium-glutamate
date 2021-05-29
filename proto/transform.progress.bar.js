const _colors = require('colors')
const cliProgress = require('cli-progress')

const noopProgressBars = {
  stop() {},
}
const noopProgressBar = {
  setTotal() {},
  update() {},
}

module.exports = {
  getProgressBars,
}

function getProgressBars(showProgressBar, schemaName) {
  if(showProgressBar) {
    const progressBars = new cliProgress.MultiBar({
      clearOnComplete: false,
      format: '{name} |' + _colors.blue('{bar}') + '| {percentage}% || {value}/{total} Messages || {schema}',
      barCompleteChar: '\u2588',
      barIncompleteChar: '\u2591',
      hideCursor: true,
    })

    const filterProgressBar = progressBars.create(0, 0, {
      name: '   filter',
      schema: schemaName,
    })
    const transformProgressBar = progressBars.create(0, 0, {
      name: 'transform',
      schema: schemaName,
    })

    return {
      progressBars,
      filterProgressBar,
      transformProgressBar,
    }
  } else {
    return {
      progressBars: noopProgressBars,
      filterProgressBar: noopProgressBar,
      transformProgressBar: noopProgressBar,
    }
  }
}