module.exports = function(channels) {
  const cmd = {
    load: (methods) => {
      channels.services.subject('methods.loaded').next(methods)
    },
    search: () => {
      channels.services.subject('search.started').next()
    },
    previousMethod: () => {
      channels.services.subject('method.selection.previous').next()
      channels.services.subject('method.selection.finished').next()
    },
    nextMethod: () => {
      channels.services.subject('method.selection.next').next()
      channels.services.subject('method.selection.finished').next()
    },
  }

  const dom = {
    search: document.querySelector('#method-search'),
    listing: document.querySelector('#method-listing'),
  }

  channels.services.subject('search.started').subscribe(() => {
    dom.search.focus()
  })

  channels.services.subject('method.selection.previous').subscribe(() => {
    let currentMethodButton = dom.listing.querySelector('.method-listing-entry.selected')
    do {
      if(currentMethodButton) {
        currentMethodButton = currentMethodButton.previousElementSibling
      }
      if(!currentMethodButton) {
        currentMethodButton = dom.listing.querySelector('.method-listing-entry:last-child')
      }
    } while(currentMethodButton && !currentMethodButton.classList.contains('selected') && (currentMethodButton.classList.contains('hidden') || !currentMethodButton.classList.contains('method-listing-entry')))

    if(currentMethodButton) {
      currentMethodButton.click()
    }
  })

  channels.services.subject('method.selection.next').subscribe(() => {
    let currentMethodButton = dom.listing.querySelector('.method-listing-entry.selected')
    do {
      if(currentMethodButton) {
        currentMethodButton = currentMethodButton.nextElementSibling
      }
      if(!currentMethodButton) {
        currentMethodButton = dom.listing.querySelector('.method-listing-entry:first-child')
      }
    } while(currentMethodButton && !currentMethodButton.classList.contains('selected') && (currentMethodButton.classList.contains('hidden') || !currentMethodButton.classList.contains('method-listing-entry')))

    if(currentMethodButton) {
      currentMethodButton.click()
    }
  })

  dom.search.addEventListener('keydown', event => {
    var key = event.which || event.keyCode;
    switch(key) {
      case 13: // Enter
      case 27: // Esc
        dom.search.value = ''
        channels.services.subject('search.changed').next(dom.search.value)
        channels.services.subject('method.selection.finished').next()
        return event.preventDefault()
      case 38: // Up
        channels.services.subject('method.selection.previous').next({})
        return event.preventDefault()
      case 40: // Down
      channels.services.subject('method.selection.next').next({})
        return event.preventDefault()
      default:
        channels.services.subject('search.changed').next(dom.search.value)
    }
  })

  dom.search.addEventListener('blur', event => {
    channels.services.subject('search.changed').next(dom.search.value)
  })

  channels.services.subject('search.changed').subscribe(searchText => {
    const searchRegex = new RegExp(searchText, "i")
    dom.listing.querySelectorAll('.method-listing-entry').forEach(methodButton => {
      if(searchText == '' || -1 < methodButton.innerText.search(searchRegex)) {
        methodButton.classList.remove('hidden')
      } else {
        methodButton.classList.add('hidden')
      }
    })
  })

  channels.services.subject('methods.loaded').subscribe(methods => {
    dom.listing.innerHTML = ''

    const methodList = document.createElement('ul')
    methodList.className = 'entry-list'
    Object.values(methods).forEach(method => {
      const methodButton = document.createElement('li')
      methodButton.className = "method-listing-entry clickable-entry tab"
      methodButton.innerHTML =
        `<h6 title="Service" style="float:right"><code>${method.serviceName}</code></h6>`+
        `<h4 title="Method"><code>${method.methodName}</code></h4>`+
        `<h5 title="Signature" style="padding:0px"><code><var>${method.requestTypeName}</var> ⇒ <var>${method.responseOf}</var> <var>${method.responseTypeName}</var></code></h5>`

      methodButton.addEventListener('click', methodButtonClickEvent => {
        const lastSelectedMethodButton = dom.listing.querySelector('.method-listing-entry.selected')
        if(lastSelectedMethodButton) {
          lastSelectedMethodButton.classList.remove('selected')
        }
        methodButton.classList.add('selected')

        channels.services.subject('method.selection.changed').next(method)
      })

      methodList.appendChild(methodButton)
    })
    dom.listing.appendChild(methodList)

    cmd.nextMethod()
  })

  return cmd
}
