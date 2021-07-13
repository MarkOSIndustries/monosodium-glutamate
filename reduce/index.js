#!/usr/bin/env node
const os = require('os')
const stream = require('stream')

const yargs = require('yargs') // eslint-disable-line
  .option('key', {
    describe: 'a property path to treat as a unique key while reducing',
    array: true,
    demandOption: true,
  })
  .option('sum', {
    describe: 'a property path to sum',
    array: true,
  })
  .option('count', {
    describe: 'a property path to count',
    array: true,
  })
  .option('min', {
    describe: 'a property path to get the minimum value of',
    array: true,
  })
  .option('max', {
    describe: 'a property path to get the maximum value of',
    array: true,
  })
  .option('alias', {
    describe: 'alias a property path for output purposes. eg: some.long.property:prop',
    array: true,
  })
  .option('labels', {
    describe: 'enable rendering of labels for unique/aggregate keys',
    boolean: true,
    default: true,
  })
  .epilogue(
    'Aggregates JSON objects from stdin and prints the result on stdout.\n'+
    'When multiple property paths are specified - order is preserved.')

function getPathElements(msg, path, keyCombination = {}) {
  let nodes = [msg]
  let pathSoFar = []
  for(const p of path.split('.')) {
    const newKeyCombination = {}
    const keyPrefix = `${p}.`
    const filterFunctions = []
    for(const k in keyCombination) {
      const filterValue = keyCombination[k]
      if(k.startsWith(keyPrefix)) {
        newKeyCombination[k.slice(keyPrefix.length)] = filterValue
      } else if(k === p) {
        filterFunctions.push(n => n[p] === filterValue)
      } else {
        filterFunctions.push(n => new Set(getPathElements(n, k)).has(filterValue))
      }
    }
    keyCombination = newKeyCombination

    nodes = nodes.filter(n => {
      for(const f of filterFunctions) {
        if(!f(n)) {
          return false
        }
      }
      return true
    }).flatMap(n => {
      if(p in n) {
        const np = n[p]
        return Array.isArray(np) ? np : [np]
      } else {
        return []
      }
    })
  }
  return nodes
}

function combinations(keys, msg) {
  if(keys.length > 0) {
    const k = keys[0]
    const theseKeyValues = getPathElements(msg, k).map(pe => ({ [k]: pe }))
    const laterKeyValues = combinations(keys.slice(1), msg)
    return theseKeyValues.flatMap(kv => laterKeyValues.map(lkv => Object.assign(kv, lkv)))
  }

  return [{}]
}

function main({key, sum, count, min, max, alias, labels}) {
  const streams = require('../streams')

  process.on('SIGINT', function() {
    // Ensure we finish writing all messages in the stream
    process.stdin.destroy()
  })

  sum = sum || []
  count = count || []
  min = min || []
  max = max || []
  alias = alias || []

  const acc = {}

  function accForKeyCombination(keyCombination) {
    let accIndex = acc
    for(const k in keyCombination) {
      const ks = JSON.stringify(keyCombination[k])
      accIndex[ks] = accIndex[ks] || {}
      accIndex = accIndex[ks]
    }
    return accIndex
  }

  process.stdin.setEncoding('utf8')

  const theStream = stream.pipeline(
      process.stdin,
      streams.readLines(),
      streams.readJsonObjects(),
      new stream.Transform({
        readableObjectMode: true,
        writableObjectMode: true,
        
        transform(msg, encoding, done) {
          const keyCombinations = combinations(key, msg)

          for(const keyCombination of keyCombinations) {
            const accIndex = accForKeyCombination(keyCombination)

            for(const s of sum) {
              accIndex[s] = accIndex[s] || {}
              accIndex[s].sum = (accIndex[s].sum || 0) +
                getPathElements(msg, s, keyCombination).reduce((a,b) => a+b, 0)
            }
            
            for(const c of count) {
              accIndex[c] = accIndex[c] || {}
              accIndex[c].count = (accIndex[c].count || 0) +
                getPathElements(msg, c, keyCombination).length
            }

            for(const m of min) {
              accIndex[m] = accIndex[m] || {}
              accIndex[m].min = [
                (accIndex[m].min || NaN),
                ...getPathElements(msg, m, keyCombination)
              ].reduce((a,b) => Number.isNaN(a) ? b : Math.min(a,b))
            }

            for(const m of max) {
              accIndex[m] = accIndex[m] || {}
              accIndex[m].max = [
                (accIndex[m].max || NaN),
                ...getPathElements(msg, m, keyCombination)
              ].reduce((a,b) => Number.isNaN(a) ? b : Math.max(a,b))
            }
          }

          done()
        }
      }),
      () => {
        function write(prefix, accNode, keys) {
          if(keys.length > 0) {
            for(const accIndex in accNode) {
              const label = labels ? `${makeKeyName(keys[0])}:` : ''
              write(`${prefix}${label}${accIndex} `, accNode[accIndex], keys.slice(1))
            }
          } else {
            process.stdout.write(prefix)
            for(const accKey in accNode) {
              if(labels) {
                process.stdout.write(makeKeyName(accKey))
                process.stdout.write(':')
              }
              process.stdout.write(JSON.stringify(accNode[accKey]))
              process.stdout.write(' ')
            }
            process.stdout.write(os.EOL)
          }
        }

        write('', acc, key)
      })

  const aliases = alias.reduce((a,b) => {
    const bBits = b.split('::')
    return Object.assign(a, { [bBits[0]]: bBits[1] })
  }, {})

  function makeKeyName(k) {
    return aliases[k] || k
  }
}

try {
  main(yargs.argv)
} catch(ex) {
  console.error(ex)
  process.exit(1)
}