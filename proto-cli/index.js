#!/usr/bin/env node
const {encode} = require('./encode')
const {decode} = require('./decode')
const env = require('../env')
var os = require('os')

const yargs = require('yargs') // eslint-disable-line
  .command('encode <schema> [encoding]', 'encode line delimited json strings to protobuf binary', (argsSpec) => {
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
      .option('delimiter', {
        alias: 'd',
        describe: 'delimiter bytes between output records (as hex string)',
        default: Buffer.from(os.EOL).toString("hex"),
        coerce: hex => Buffer.from(hex, "hex")
      })
  }, ({schema, encoding, delimiter, protobufs}) => {
    encode(schema, encoding, delimiter, protobufs)
  })
  .command('decode <schema> [encoding]', 'decode line delimited protobuf binary encodings to json strings', (argsSpec) => {
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
    .option('delimiter', {
      alias: 'd',
      describe: 'delimiter bytes between output records (as hex string)',
      default: Buffer.from(os.EOL).toString("hex"),
      coerce: hex => Buffer.from(hex, "hex")
    })
  }, ({schema, encoding, delimiter, protobufs}) => {
    decode(schema, encoding, delimiter, protobufs)
  })
  .option('protobufs', {
    describe: 'path which contains protobuf schemas (env PROTO_HOME to override)',
    default: env.PROTO_HOME,
  })
  .wrap(null)
  .env('MSG_')

const argv = yargs.argv

if(!argv._[0]) {// no command?
  yargs.showHelp()
}
