#!/usr/bin/env node
const parquet = require('parquetjs-lite')
const protobuf = require('../protobuf')(require('protobufjs'))
const varint = require('../varint')(require('varint'))
const {supportedEncodings, InputStreamDecoder, OutputStreamEncoder} = require('../protobuf.cli.encodings')
const {getJsonRenderer} = require('../cli.json')
const {protobufSchemaToParquetSpec} = require('./schema.conversion')
const stream = require('stream')
const streams = require('../streams')
const env = require('../env')
var os = require('os')

process.on('SIGINT', function() {
  // Ensure we finish writing all messages in the stream
  process.stdin.destroy()
})

const yargs = require('yargs') // eslint-disable-line
  .command('read <file> <schema> <output>', 'transform a parquet file to protobuf records on stdout', (argsSpec) => {
    argsSpec
      .positional('file', {
        describe: 'the file to read parquet records from',
      })
      .positional('schema', {
        describe: 'protobuf schema to interpret rows as',
      })
      .positional('output', {
        describe: 'the output format to use',
        choices: supportedEncodings,
      })
      .option('arrays', {
        describe: 'how should arrays be represented (Spark ArrayType-compatible format is more verbose)',
        choices: ['spark', 'simple'],
        default: 'spark',
      })
      .option('enums', {
        describe: 'how should enums be represented',
        choices: ['strings', 'integers'],
        default: 'integers',
      })
      // TODO: support column filtering
      addEncodingOptions(argsSpec)
  }, argv => {
    const index = indexProtobufs(argv.protobufs)
    if(!index.messages.hasOwnProperty(argv.schema)) throw `Schema ${argv.schema} not found. Try >proto schemas`
    const schema = index.messages[argv.schema]

    const parquetSpec = protobufSchemaToParquetSpec(schema, {compression: argv.compression, enumsAsStrings: argv.enums === 'strings', sparkCompatibleMode: argv.arrays === 'spark' })
    
    const outputStreamEncoder = new OutputStreamEncoder(process.stdout, schema, argv.output, coercePrefix(argv.prefix), argv.delimiter, getJsonRenderer(process.stdout.isTTY))

    const outputStream = new stream.Transform({
      transform(message, encoding, done) {
        this.push(message)
        done()
      },
    })

    stream.pipeline(
      outputStream,
      outputStreamEncoder.makeOutputTransformer(),
      outputStreamEncoder.getOutputStream(),
      () => {})
    
    parquet.ParquetReader.openFile(argv.file).then(parquetReader => {
      const cursor = parquetReader.getCursor()
      function onNext(parquetObject) {
        if(parquetObject) {
          const jsonObject = parquetSpec.parquetToProto(parquetObject)
          outputStream.write(outputStreamEncoder.marshalJsonObject(jsonObject))
          cursor.next().then(onNext).catch(err => console.error(err))
        } else {
          parquetReader.close().then(() => {
            outputStream.end()
          })
        }
      }
      cursor.next().then(onNext).catch(err => console.error(err))

      process.on('SIGINT', function() {
        parquetReader.close().then(() => {
          process.exit()
        })
      })
    })
    .catch(err => console.error(err))
  })
  .command('write <file> <schema> <input>', 'transform protobuf records from stdin to a parquet file', (argsSpec) => {
    argsSpec
      .positional('file', {
        describe: 'the file to write parquet records to',
      })
      .positional('schema', {
        describe: 'protobuf schema to interpret rows as',
      })
      .positional('input', {
        describe: 'the input format to expect',
        choices: supportedEncodings,
      })
      .option('arrays', {
        describe: 'how should arrays be represented (Spark ArrayType-compatible format is more verbose)',
        choices: ['spark', 'simple'],
        default: 'spark',
      })
      .option('enums', {
        describe: 'how should enums be represented',
        choices: ['strings', 'integers'],
        default: 'integers',
      })
      .option('compression', {
        alias: 'c',
        describe: 'The compression type to use for writing parquet',
        choices: ['UNCOMPRESSED', 'GZIP', 'SNAPPY', 'BROTLI'],
        default: 'SNAPPY',
      })
      .option('pageformat', {
        describe: 'The page format version to use',
        choices: ['v1', 'v2'],
        default: 'v2',
      })
      addEncodingOptions(argsSpec)
  }, argv => {
    const index = indexProtobufs(argv.protobufs)
    if(!index.messages.hasOwnProperty(argv.schema)) throw `Schema ${argv.schema} not found. Try >proto schemas`
    const schema = index.messages[argv.schema]

    const parquetSpec = protobufSchemaToParquetSpec(schema, {compression: argv.compression, enumsAsStrings: argv.enums === 'strings', sparkCompatibleMode: argv.arrays === 'spark' })
    const parquetSchema = new parquet.ParquetSchema(parquetSpec.schema)
    parquet.ParquetWriter.openFile(parquetSchema, argv.file, { compression: argv.compression, useDataPageV2: argv.pageformat === 'v2' }).then(parquetWriter => {
      const inputStreamDecoder = new InputStreamDecoder(process.stdin, schema, argv.input, coercePrefix(argv.prefix), argv.delimiter)
      stream.pipeline(
        inputStreamDecoder.getInputStream(),
        inputStreamDecoder.makeInputTransformer(),
        new stream.Transform({
          readableObjectMode: true,
          
          transform(message, encoding, done) {
            const jsonObject = inputStreamDecoder.unmarshalJsonObject(message)
            const parquetObject = parquetSpec.protoToParquet(jsonObject)
            parquetWriter.appendRow(parquetObject).then(() => done())
          },
        }),
        err => {
          parquetWriter.close().then(() => {
            process.exit()
          })
        })

      process.on('SIGINT', function() {
        inputStreamDecoder.getInputStream().destroy()
      })
    })
    .catch(err => console.error(err))
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
      describe: 'delimiter bytes between records (as hex string) - doesn\'t apply to binary encoding',
      default: Buffer.from(os.EOL).toString("hex"),
      coerce: hex => Buffer.from(hex, "hex"),
    })
    .option('prefix', {
      describe: 'length prefix format - only applies to binary encoding',
      default: 'varint',
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
        'varint',
      ]
    })
}

function coercePrefix(prefixFormat) {
  switch(prefixFormat) {
    case 'varint':
      return {
        lengthPrefixReader: varint.varIntLengthPrefixReader,
        lengthPrefixWriter: varint.varIntLengthPrefixWriter,
      }
    default:
      return {
        lengthPrefixReader: streams.simpleLengthPrefixReader(prefixFormat),
        lengthPrefixWriter: streams.simpleLengthPrefixWriter(prefixFormat),
      }
  }
}

function indexProtobufs(userSuppliedProtobufPaths) {
  const messages =  protobuf.loadFromPaths([
    protobuf.getGoogleSchemasPath(),
    protobuf.getMSGSchemasPath(),
    ...userSuppliedProtobufPaths,
  ])
  return protobuf.makeFlatIndex(messages)
}

try {
  const argv = yargs.argv
  if(!argv._[0]) {// no command?
    yargs.showHelp()
  }
} catch(ex) {
  console.error(ex)
  process.exit(1)
}