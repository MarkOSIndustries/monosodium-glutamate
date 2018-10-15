#!/usr/bin/env node
const {transform} = require('./transform')
const {schemas} = require('./schemas')
const {coerceFilter} = require('./filter')
const {coerceShape} = require('./shape.js')
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
        describe: 'the input format to expect. spam/generator ignore stdin and generate pseudo-random, valid records',
        choices: [
          'json',
          'json_hex',
          'json_base64',
          'base64',
          'hex',
          'binary',
        ],
      })
      .positional('output', {
        describe: 'the output format to use',
        choices: [
          'json',
          'json_hex',
          'json_base64',
          'base64',
          'hex',
          'binary',
        ],
      })
      addTransformOptions(argsSpec)
  }, argv => {
    transform(argv)
  })
  .command('spam <schema> <output>', 'generate pseudo-random protobuf records to stdout', (argsSpec) => {
    argsSpec
      .positional('schema', {
        describe: 'protobuf schema to generate messages for',
      })
      .positional('output', {
        describe: 'the output format to use',
        choices: [
          'json',
          'json_hex',
          'json_base64',
          'base64',
          'hex',
          'binary',
        ],
      })
      addTransformOptions(argsSpec)
  }, argv => {
    transform(Object.assign(argv, {input: 'generator'}))
  })
  .command('schemas [query]', 'list all known schemas', (argsSpec) => {
    argsSpec
      .positional('query', {
        describe: 'show only schemas containing the given substring (case insensitive)',
        default: '',
        coerce: query => new RegExp(query, 'i'),
      })
  }, argv => {
    schemas(argv)
  })
  .option('protobufs', {
    describe: 'paths which contain protobuf schemas (comma separated, env PROTO_HOME to override)',
    default: env.PROTO_HOME,
    coerce: x => x.split(','),
  })
  .wrap(null)
  .env('PROTO_')

function addTransformOptions(argsSpec) {
  argsSpec
    .option('filter', {
      alias: 'f',
      describe: 'filter records by matching field values against a partially specified json payload',
      default: null,
      coerce: coerceFilter,
    })
    .option('shape', {
      alias: 's',
      describe: 'project JSON records through an arbitrary JS snippet (currently only works for input=generator)',
      default: null,
      coerce: coerceShape,
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
}

const argv = yargs.argv

if(!argv._[0]) {// no command?
  yargs.showHelp()
}
