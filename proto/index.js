#!/usr/bin/env node
const {transform} = require('./transform')
const {schemas} = require('./schemas')
const {coerceFilter} = require('./filter')
const {coerceTemplate} = require('./template.js')
const env = require('../env')
var os = require('os')

const yargs = require('yargs') // eslint-disable-line
  .command('transform <schema> <input> <output>', 'transform protobuf records from stdin to stdout', (argsSpec) => {
    argsSpec
      .positional('schema', {
        describe: 'protobuf schema to interpret messages as',
      })
      .positional('input', {
        describe: 'the input format to expect. generator means generate pseudo-random, valid records',
        choices: [
          'json',
          'base64',
          'hex',
          'binary',
          'generator',
        ],
      })
      .positional('output', {
        describe: 'the output format to use',
        choices: [
          'json',
          'base64',
          'hex',
          'binary',
        ],
      })
  }, argv => {
    transform(argv)
  })
  .command('schemas [query]', 'list all known schemas', (argsSpec) => {
    argsSpec
      .positional('query', {
        describe: 'show only schemas containing the given substring (case insensitive)',
        default: '',
        coerce: query => new RegExp(query, 'gi'),
      })
  }, argv => {
    schemas(argv)
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
