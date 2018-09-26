# Monosodium Glutamate

A collection of tools for working with protobuf messages in datastores, over GRPC, and in Kafka.

## Index
- [GRPC-GUI](grpc-gui/README.md) See and type JSON, send/receive GRPC protobufs
- [kat](kat/README.md) Produce/consume to/from kafka via piped stdin/out
- [proto](proto/README.md) Encode/decode JSON to/from protobufs using various encodings

## Building
This repo is set up so that you can build everything with one command.
You'll need to have some tools installed:
- [Git](https://git-scm.com)
- [Gradle](https://gradle.org/install/)
- [Node.js v10.x](https://nodejs.org/en/download/)
- [Java 8](http://openjdk.java.net/install/)

```bash
# Clone this repository
git clone https://github.com/markosindustries/monosodium-glutamate
# Go into the repository
cd monosodium-glutamate
# Build
gradle build
```

## Example Usage
These tools are at their best when combined together. Here are some examples of real use cases.

*All commands work the same on windows, just replace `.sh` with `.bat`*

#### Tail a Kafka topic in realtime as JSON
Consume a topic called `my.Topic` and deserialise the values as `my.MessageType` protobuf messages
```bash
./bin/kat.sh consume my.Topic binary \
  --from latest --until forever |\
  ./bin/proto.sh transform my.MessageType binary json
```

#### Fill a Kafka topic with valid protobuf messages
Produce protobuf messages of type `my.MessageType`, serialise them, and send to topic `my.Topic` with random keys
```bash
./bin/proto.sh spam my.MessageType binary | \
  ./bin/kat.sh produce my.Topic binary
```

### Extract a partition time range from Kafka as insert statements
Construct a file of INSERT statements from `my.MessageType` protobuf messages in topic `my.Topic` filtering to only records on partition #3
```bash
./bin/kat.sh consume my.Topic msg.TypedKafkaRecord \
  --from 1636900200000 --until 1636900800000 \
  --schema my.MessageType |\
  ./bin/proto.sh transform msg.TypedKafkaRecord json -f "{\"partition\":3}" \
  -t "INSERT INTO SomeTable(Id,Name) VALUES ('${msg.value.id}','${msg.value.name}')" > /tmp/script.sql
```

### Mirror a topic to another topic/cluster
Use the same keys/values produced to clusterA to populate a topic on clusterB
```bash
bin\kat consume my.TopicA -b clusterA msg.KafkaRecord | \
  bin\kat produce my.TopicB -b clusterB msg.KafkaRecord
```
