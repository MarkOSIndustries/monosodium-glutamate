#!/usr/bin/env node
const {transform} = require('./transform')
const {coerceFilter} = require('./filter')
const {coerceTemplate} = require('./template.js')
const env = require('../env')
var os = require('os')

const formats = {
  'json': 'lineDelimitedJson',
  'encoded': 'lineDelimitedEncodedBinary',
  'binary': 'lengthPrefixedBinary',
  'generator': 'generator',
}

const yargs = require('yargs') // eslint-disable-line
  .command('transform <schema> <input> <output>', 'transform protobuf records from stdin to stdout', (argsSpec) => {
    argsSpec
      .positional('schema', {
        describe: 'protobuf schema to interpret messages as',
      })
      .positional('input', {
        describe: 'the input format to expect',
        choices: [
          'json',
          'encoded',
          'binary',
          'generator',
        ]
      })
      .positional('output', {
        describe: 'the output format to use',
        choices: [
          'json',
          'encoded',
          'binary',
        ]
      })
  }, argv => {
    transform(formats[argv.input], formats[argv.output], argv)
  })
  .command('encode <schema>', 'line-delimited json strings => delimited, string-encoded protobuf binary records', (argsSpec) => {
    argsSpec
      .positional('schema', {
        describe: 'protobuf schema to encode messages with',
      })
  }, argv => {
    transform('lineDelimitedJson', 'lineDelimitedEncodedBinary', argv)
  })
  .command('decode <schema>', 'line-delimited, string-encoded protobuf binary records => delimited json strings', (argsSpec) => {
    argsSpec
    .positional('schema', {
      describe: 'protobuf schema to encode messages with',
    })
  }, argv => {
    transform('lineDelimitedEncodedBinary', 'lineDelimitedJson', argv)
  })
  .command('serialise <schema>', 'line-delimited json strings => length-prefixed protobuf binary records', (argsSpec) => {
    argsSpec
    .positional('schema', {
      describe: 'protobuf schema to serialise messages with',
    })
  }, argv => {
    transform('lineDelimitedJson', 'lengthPrefixedBinary', argv)
  })
  .command('deserialise <schema>', 'length-prefixed protobuf binary records => delimited json strings', (argsSpec) => {
    argsSpec
    .positional('schema', {
      describe: 'protobuf schema to deserialise messages with',
    })
  }, argv => {
    transform('lengthPrefixedBinary', 'lineDelimitedJson', argv)
  })
  .command('spam <schema>', 'generates valid, pseudo-random records as delimited json strings', (argsSpec) => {
    argsSpec
    .positional('schema', {
      describe: 'protobuf schema to generate messages with',
    })
  }, argv => {
    transform('generator', 'lineDelimitedJson', argv)
  })
  .option('filter', {
    alias: 'f',
    describe: 'filter records by matching field values against a partially specified json payload',
    default: null,
    coerce: coerceFilter,
  })
  .option('template', {
    alias: 't',
    describe: 'project records through a string template (uses standard js string interpolation syntax)',
    default: null,
    coerce: coerceTemplate,
  })
  .option('delimiter', {
    alias: 'd',
    describe: 'delimiter bytes between output records (as hex string)',
    default: Buffer.from(os.EOL).toString("hex"),
    coerce: hex => Buffer.from(hex, "hex"),
  })
  .option('prefix', {
    alias: 'p',
    describe: 'length prefix format',
    default: 'Int32BE',
    choices: [
      'Int32LE',
      'Int32BE',
      'Int16LE',
      'Int16BE',
      'Int8',
      'UInt32LE',
      'UInt32BE',
      'UInt16LE',
      'UInt16BE',
      'UInt8',
    ]
  })
  .option('encoding', {
    alias: 'e',
    describe: 'the encoding to use for string-encoded binary',
    default: 'hex',
    choices: ['base64', 'hex'],
  })
  .option('protobufs', {
    describe: 'path which contains protobuf schemas (env PROTO_HOME to override)',
    default: env.PROTO_HOME,
  })
  .wrap(null)
  .env('PROTO_')

const argv = yargs.argv

if(!argv._[0]) {// no command?
  yargs.showHelp()
}
