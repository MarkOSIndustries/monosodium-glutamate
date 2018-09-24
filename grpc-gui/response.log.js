module.exports = domContainer => {
  domContainer.innerHTML = ''

  return {
    reset: () => {
      domContainer.innerHTML = ''
    },
    render: responses =>  {
      var fragment = document.createDocumentFragment()
      responses.forEach(response => {
        const responseElement = document.createElement('pre')
        responseElement.innerHTML = `<code>${JSON.stringify(response, undefined, '  ')}</code>`
        fragment.appendChild(responseElement)
        fragment.appendChild(document.createElement('hr'))
      })
      domContainer.appendChild(fragment)
    },
  }
}
