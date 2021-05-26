#!/usr/bin/env node
const protobuf = require('../protobuf')(require('protobufjs'))
const {supportedEncodings, InputStreamDecoder, OutputStreamEncoder, MockInputStreamDecoder} = require('./encodings')
const {transformInSingleProcess} = require('./transform.single.process')
const {transformInParentProcess, transformInForkedProcess} = require('./transform.multi.process')
const {invoke, transformToRequestResponsePairs, transformToResponsesOnly} = require('./invoke')
const {schemas} = require('./schemas')
const {services} = require('./services')
const {coerceFilter} = require('./filter')
const {coerceShape} = require('./shape.js')
const {coerceTemplate, coerceTTY} = require('./template.js')
const env = require('../env')
var os = require('os')

process.on('SIGINT', function() {
  // Ensure we finish writing all messages in the stream
  process.stdin.destroy()
})

const yargs = require('yargs') // eslint-disable-line
  .command('transform <schema> <input> <output>', 'transform protobuf records from stdin to stdout', (argsSpec) => {
    argsSpec
      .positional('schema', {
        describe: 'protobuf schema to interpret messages as',
      })
      .positional('input', {
        describe: 'the input format to expect',
        choices: supportedEncodings,
      })
      .positional('output', {
        describe: 'the output format to use',
        choices: supportedEncodings,
      })
      .option('concurrency', {
        alias: 'c',
        describe: 'Should we use multiple processes (order will be maintained either way)',
        choices: ['multi', 'single'],
        default: 'multi',
      })
      .option('parallelism', {
        alias: 'p',
        describe: 'How many workers should we use when running in multi-process mode. (defaults to machine CPU core count)',
        default: os.cpus().length,
        coerce: x => Number(x)
      })
      addEncodingOptions(argsSpec)
      addTransformOptions(argsSpec)
  }, argv => {
    const index = indexProtobufs(argv.protobufs)
    if(!index.messages.hasOwnProperty(argv.schema)) throw `Schema ${argv.schema} not found. Try >proto schemas`
    const schema = index.messages[argv.schema]

    if(argv.concurrency == 'single') {
      transformInSingleProcess(
        new InputStreamDecoder(process.stdin, schema, argv.input, argv.prefix, argv.delimiter),
        new OutputStreamEncoder(process.stdout, schema, argv.output, argv.prefix, argv.delimiter, coerceTemplate(argv.template, argv.tty)),
        argv.filter, argv.shape)
    } else {
      if(isNaN(argv.parallelism)) {
        throw 'Parallelism must be a number'
      }
      transformInParentProcess(
        new InputStreamDecoder(process.stdin, schema, argv.input, argv.prefix, argv.delimiter),
        new OutputStreamEncoder(process.stdout, schema, argv.output, argv.prefix, argv.delimiter, coerceTemplate(argv.template, argv.tty)),
        argv.filter, argv.shape, argv.parallelism, ['transform.forked', coerceTTY(argv.tty), ...process.argv.slice(3)])
    }
  })
  .command('transform.forked <isttyparent> <schema> <input> <output>', false, (argsSpec) => {
    argsSpec
      .positional('isttyparent', {
        describe: 'Should we assume stdout is a TTY?',
        choices: ['y','n'],
        coerce: (x) => x === 'true' ? 'y' : 'n',
      })
      .positional('schema', {
        describe: 'protobuf schema to interpret messages as',
      })
      .positional('input', {
        describe: 'the input format to expect',
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

    transformInForkedProcess(
      new InputStreamDecoder(process.stdin, schema, argv.input, argv.prefix, argv.delimiter),
      new OutputStreamEncoder(process.stdout, schema, argv.output, argv.prefix, argv.delimiter, coerceTemplate(argv.template, argv.isttyparent)),
      argv.filter, argv.shape)
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
      .option('requests', {
        describe: 'what to do with requests when parsing responses. pair will emit msg.RequestResponsePair instead of the response type directly',
        choices: ['discard', 'pair'],
        default: 'discard',
      })
      addEncodingOptions(argsSpec)
  }, argv => {
    const index = indexProtobufs(argv.protobufs)
    if(!index.services.hasOwnProperty(argv.service)) throw `Service ${argv.service} not found. Try >proto services`
    const service = index.services[argv.service]
    const methods = protobuf.describeServiceMethods(service)
    if(!methods.hasOwnProperty(argv.method)) throw `Method ${argv.service}.${argv.method} not found. Try >proto services ${argv.service}`
    const method = methods[argv.method]
    const useRequestPairing = argv.requests === 'pair'
    const transformRequestResponse = useRequestPairing
        ? transformToRequestResponsePairs(index)
        : transformToResponsesOnly(index)
    const responseType = useRequestPairing
        ? index.messages['msg.RequestResponsePair']
        : method.responseType

    invoke(method,
      new InputStreamDecoder(process.stdin, method.requestType, argv.input, argv.prefix, argv.delimiter),
      new OutputStreamEncoder(process.stdout, responseType, argv.output, argv.prefix, argv.delimiter, coerceTemplate(argv.template, argv.tty)),
      argv.host, argv.port, argv.timeout, transformRequestResponse)
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

    transformInSingleProcess(
      new MockInputStreamDecoder(schema),
      new OutputStreamEncoder(process.stdout, schema, argv.output, argv.prefix, argv.delimiter, coerceTemplate(argv.template, argv.tty)),
      argv.filter, argv.shape)
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
  .completion()

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
    })
    .option('tty', {
      describe: 'Should we assume stdout is a TTY? (autodetects by default)',
      choices: ['y','n'],
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
