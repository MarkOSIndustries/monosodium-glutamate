#!/usr/bin/env node
const {encode} = require('./encode')
const {decode} = require('./decode')
const {serialise} = require('./serialise')
const {deserialise} = require('./deserialise')
const {spam} = require('./spam')
const env = require('../env')
var os = require('os')

const yargs = require('yargs') // eslint-disable-line
  .command('encode <schema> [encoding]', 'line-delimited json strings => delimited, string-encoded protobuf binary records', (argsSpec) => {
    argsSpec
      .positional('schema', {
        describe: 'protobuf schema to encode messages with',
      })
      .positional('encoding', {
        alias: 'e',
        describe: 'the encoding to use for binary output',
        default: 'hex',
        choices: ['base64', 'hex'],
      })
  }, ({schema, encoding, delimiter, protobufs, filter, template}) => {
    encode(schema, encoding, delimiter, protobufs, filter, template)
  })
  .command('decode <schema> [encoding]', 'line-delimited, string-encoded protobuf binary records => delimited json strings', (argsSpec) => {
    argsSpec
    .positional('schema', {
      describe: 'protobuf schema to encode messages with',
    })
    .positional('encoding', {
      alias: 'e',
      describe: 'the encoding of input records',
      default: 'hex',
      choices: ['base64', 'hex'],
    })
  }, ({schema, encoding, delimiter, protobufs, filter, template}) => {
    decode(schema, encoding, delimiter, protobufs, filter, template)
  })
  .command('serialise <schema>', 'line-delimited json strings => length-prefixed protobuf binary records', (argsSpec) => {
    argsSpec
    .positional('schema', {
      describe: 'protobuf schema to serialise messages with',
    })
  }, ({schema, prefix, protobufs, filter, template}) => {
    serialise(schema, prefix, protobufs, filter, template)
  })
  .command('deserialise [schema]', 'length-prefixed protobuf binary records => delimited json strings', (argsSpec) => {
    argsSpec
    .positional('schema', {
      describe: 'protobuf schema to deserialise messages with',
      default: 'msg.TypedKafkaRecord',
    })
  }, ({schema, prefix, delimiter, protobufs, filter, template}) => {
    deserialise(schema, prefix, delimiter, protobufs, filter, template)
  })
  .command('spam <schema>', 'generates valid, pseudo-random records as delimited json strings', (argsSpec) => {
    argsSpec
    .positional('schema', {
      describe: 'protobuf schema to generate messages with',
    })
  }, ({schema, delimiter, protobufs, filter, template}) => {
    spam(schema, delimiter, protobufs, filter, template)
  })
  .option('filter', {
    alias: 'f',
    describe: 'filter records by matching field values against a sample json payload',
    default: '{}',
    coerce: jsonString => JSON.parse(jsonString)
  })
  .option('template', {
    alias: 't',
    describe: 'project records through a string template (uses standard js string interpolation syntax)',
    default: null,
    coerce: templateString => {
      if(templateString) {
        return msg => eval('`'+templateString.replace(/`/,'\\`')+'`')
      } else {
        return x => x
      }
    }
  })
  .option('delimiter', {
    alias: 'd',
    describe: 'delimiter bytes between output records (as hex string)',
    default: Buffer.from(os.EOL).toString("hex"),
    coerce: hex => Buffer.from(hex, "hex")
  })
  .option('prefix', {
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
