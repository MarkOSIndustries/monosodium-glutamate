#!/usr/bin/env node
const {consume} = require('./consume')
const {produce} = require('./produce')
const env = require('../env')

const yargs = require('yargs') // eslint-disable-line
  .command('consume <topic> <schema> [seek] [timestamp]', 'consume messages from kafka', (argsSpec) => {
    argsSpec
      .positional('topic', {
        describe: 'kafka topic to consume from',
      })
      .positional('schema', {
        describe: 'protobuf schema to decode messages with',
      })
      .positional('seek', {
        describe: 'where to start consuming from',
        default: 'earliest',
        choices: [
          'earliest',
          'latest',
          'timestamp',
        ],
      })
      .positional('timestamp', {
        describe: 'timestamp (ms) to use when seeking by timestamp',
      })
  }, (argv) => {
    consume(argv.kafkaBrokers, argv.protobufs, [argv.topic], argv.schema, argv.seek, argv.timestamp)
  })
  .command('produce <topic> <schema>', 'produce messages to kafka', (argsSpec) => {
    argsSpec
      .positional('topic', {
        describe: 'kafka topic to produce to',
      })
      .positional('schema', {
        describe: 'protobuf schema to encode messages with',
      })
  }, (argv) => {
    produce(argv.kafkaBrokers, argv.protobufs, argv.topic, argv.schema)
  })
  .option('kafkaBrokers', {
    describe: 'comma separated kafka broker addresses (env KAFKA_BROKERS to override)',
    default: env.KAFKA_BROKERS,
    coerce: (kafkaBrokers) => kafkaBrokers.split(',').map(b => b.match(/([^:]*):(\d*)/)).map(b => ({ host: b[1], port: b[2] })),
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
