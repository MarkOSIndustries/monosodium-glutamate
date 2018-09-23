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
        responseElement.innerText = JSON.stringify(response, undefined, '  ')
        fragment.appendChild(responseElement)
        fragment.appendChild(document.createElement('hr'))
      })
      domContainer.appendChild(fragment)
    },
  }
}
