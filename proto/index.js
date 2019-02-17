#!/usr/bin/env node
const protobuf = require('../protobuf')(require('protobufjs'))
const {supportedEncodings, InputStreamDecoder, OutputStreamEncoder, MockInputStreamDecoder} = require('./encodings')
const {transform} = require('./transform')
const {invoke} = require('./invoke')
const {schemas} = require('./schemas')
const {services} = require('./services')
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
        choices: supportedEncodings,
      })
      .positional('output', {
        describe: 'the output format to use',
        choices: supportedEncodings,
      })
      addEncodingOptions(argsSpec)
      addTransformOptions(argsSpec)
  }, argv => {
    const index = indexProtobufs(argv.protobufs)
    if(!index.messages.hasOwnProperty(argv.schema)) throw `Schema ${argv.schema} not found. Try >proto schemas`
    const schema = index.messages[argv.schema]

    transform(
      new InputStreamDecoder(process.stdin, schema, argv.input, argv.prefix, argv.delimiter),
      new OutputStreamEncoder(process.stdout, schema, argv.output, argv.prefix, argv.delimiter, argv.template),
      argv.filter, argv.shape, argv.template)
  })
  .command('invoke <service> <method> <input> <output>', 'invoke a GRPC method by reading requests from stdin and writing responses to stdout', (argsSpec) => {
    argsSpec
      .positional('service', {
        describe: 'GRPC service containing the method to invoke',
      })
      .positional('method', {
        describe: 'GRPC service method to invoke',
      })
      .positional('input', {
        describe: 'the input format to expect for requests',
        choices: supportedEncodings,
      })
      .positional('output', {
        describe: 'the output format to use for responses',
        choices: supportedEncodings,
      })
      .option('host', {
        alias: 'h',
        describe: 'the host to connect to',
        default: 'localhost',
      })
      .option('port', {
        alias: 'p',
        describe: 'the port to connect to',
        default: 8082,
      })
      .option('timeout', {
        describe: 'the GRPC deadline in minutes',
        default: 5,
      })
      addEncodingOptions(argsSpec)
  }, argv => {
    const index = indexProtobufs(argv.protobufs)
    if(!index.services.hasOwnProperty(argv.service)) throw `Service ${argv.service} not found. Try >proto services`
    const service = index.services[argv.service]
    const methods = protobuf.describeServiceMethods(service)
    if(!methods.hasOwnProperty(argv.method)) throw `Method ${argv.service}.${argv.method} not found. Try >proto services ${argv.service}`
    const method = methods[argv.method]

    invoke(method,
      new InputStreamDecoder(process.stdin, method.requestType, argv.input, argv.prefix, argv.delimiter),
      new OutputStreamEncoder(process.stdout, method.responseType, argv.output, argv.prefix, argv.delimiter, argv.template),
      argv.host, argv.port, argv.timeout)
  })
  .command('spam <schema> <output>', 'generate pseudo-random protobuf records to stdout', (argsSpec) => {
    argsSpec
      .positional('schema', {
        describe: 'protobuf schema to generate messages for',
      })
      .positional('output', {
        describe: 'the output format to use',
        choices: supportedEncodings,
      })
      addEncodingOptions(argsSpec)
      addTransformOptions(argsSpec)
  }, argv => {
    const index = indexProtobufs(argv.protobufs)
    if(!index.messages.hasOwnProperty(argv.schema)) throw `Schema ${argv.schema} not found. Try >proto schemas`
    const schema = index.messages[argv.schema]

    transform(
      new MockInputStreamDecoder(schema),
      new OutputStreamEncoder(process.stdout, schema, argv.output, argv.prefix, argv.delimiter, argv.template),
      argv.filter, argv.shape, argv.template)
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
  .command('services [query]', 'list all known GRPC services', (argsSpec) => {
    argsSpec
      .positional('query', {
        describe: 'show only services containing the given substring (case insensitive)',
        default: '',
        coerce: query => new RegExp(query, 'i'),
      })
  }, argv => {
    services(argv)
  })
  .option('protobufs', {
    describe: 'paths which contain protobuf schemas (comma separated, env PROTO_HOME to override)',
    default: env.PROTO_HOME,
    coerce: x => x.split(','),
  })
  .wrap(null)
  .env('PROTO_')

function addEncodingOptions(argsSpec) {
  argsSpec
    .option('delimiter', {
      alias: 'd',
      describe: 'delimiter bytes between output records (as hex string) - doesn\'t apply to binary output',
      default: Buffer.from(os.EOL).toString("hex"),
      coerce: hex => Buffer.from(hex, "hex"),
    })
    .option('prefix', {
      describe: 'length prefix format - only applies to binary output',
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
    .option('template', {
      alias: 't',
      describe: 'project records through a string template (uses standard js string interpolation syntax) - only applies to json output',
      default: null,
      coerce: coerceTemplate,
    })
}

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
}

const argv = yargs.argv

if(!argv._[0]) {// no command?
  yargs.showHelp()
}

function indexProtobufs(userSuppliedProtobufPaths) {
  const messages =  protobuf.loadFromPaths([
    protobuf.getGoogleSchemasPath(),
    protobuf.getMSGSchemasPath(),
    ...userSuppliedProtobufPaths,
  ])
  return protobuf.makeFlatIndex(messages)
}
