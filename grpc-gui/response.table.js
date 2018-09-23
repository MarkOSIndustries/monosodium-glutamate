module.exports = domContainer => {
  domContainer.innerHTML = ''
  const dom = {
    table: document.createElement('table'),
    tableHead: document.createElement('thead'),
    tableHeadRow: document.createElement('tr'),
    tableBody: document.createElement('tbody'),
  }
  dom.tableHead.appendChild(dom.tableHeadRow)
  dom.table.appendChild(dom.tableHead)
  dom.table.appendChild(dom.tableBody)
  domContainer.appendChild(dom.table)

  const state = {
    headings: []
  }

  return {
    reset: () => {
      dom.tableHeadRow.innerHTML = ''
      dom.tableBody.innerHTML = ''
      state.headings = []
    },
    render: responses =>  {
      if(!responses.length) return
      if(!state.headings.length) {
        state.headings.push(...Object.keys(responses[0]).map(key => {
          switch(typeof responses[0][key]) {
            case 'object': return {
              key,
              align: 'left',
              render: response => JSON.stringify(response[key], undefined, '  '),
            }
            default: return {
              key,
              align: 'right',
              render: response => response[key],
            }
          }
        }))
        state.headings.forEach(heading => {
          const tableHeadCell = document.createElement('th')
          tableHeadCell.innerHTML = heading.key
          tableHeadCell.style['text-align'] = heading.align
          dom.tableHeadRow.appendChild(tableHeadCell)
        })
      }

      var fragment = document.createDocumentFragment()
      responses.forEach(response => {
        const row = document.createElement('tr')
        state.headings.forEach(heading => {
          const cell = document.createElement('td')
          cell.innerHTML = `<pre>${heading.render(response)}</pre>`
          cell.style['text-align'] = heading.align
          row.appendChild(cell)
        })
        fragment.appendChild(row)
      })
      dom.tableBody.appendChild(fragment)
    },
  }
}
