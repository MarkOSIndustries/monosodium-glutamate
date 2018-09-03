const PROTO_HOME = process.env.PROTO_HOME || process.cwd()
const KAFKA_BROKERS = process.env.KAFKA_BROKERS || 'localhost:9092'

module.exports = {
  PROTO_HOME,
  KAFKA_BROKERS,
}
