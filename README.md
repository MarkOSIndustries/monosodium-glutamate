# Monosodium Glutamate

A collection of tools for working with protobuf messages in datastores, over GRPC, and in Kafka.

## Index
- [GRPC-GUI](grpc-gui/README.md) See and type JSON, send/receive GRPC protobufs
- [kat](kat/README.md) Produce/consume to/from kafka via piped stdin/out
- [proto](proto/README.md) Encode/decode JSON to/from protobufs using various encodings
- [kgb](kat/README.md) Run a GRPC gateway to perform simple Kafka operations
- [qs](qs/README.md) A useful tool for piping the output from other commands into a rocksdb

## Running
The command line tools (everything except for [GRPC-GUI](grpc-gui/README.md)) are packaged into a [Docker image](https://hub.docker.com/r/markosindustries/monosodium-glutamate) image.
To run in interactive mode, with your protobuf schemas accessible to the tools:
```bash
docker run --mount type=bind,src=/your/proto/schemas,dst=/home/schemas -it markosindustries/monosodium-glutamate
```

## Building
Each of the tools' README explains how to build it independently, but if you just want to build the docker image yourself:
```bash
# Clone this repository
git clone https://github.com/markosindustries/monosodium-glutamate
# Go into the repository
cd monosodium-glutamate
# Build the docker image
docker build . -f docker/Dockerfile -t msg
```

## Example Usage
These tools are at their best when combined together. Here are some examples of real use cases.

#### Tail a Kafka topic in realtime as JSON
Consume a topic called `my.Topic` and deserialise the values as `my.MessageType` protobuf messages
```bash
kat consume my.Topic binary \
  --from latest --until forever |\
  proto transform my.MessageType binary json
```

#### Fill a Kafka topic with valid protobuf messages
Produce protobuf messages of type `my.MessageType`, serialise them, and send to topic `my.Topic` with random keys
```bash
proto spam my.MessageType binary | \
  kat produce my.Topic binary
```

### Extract a partition time range from Kafka as insert statements
Construct a file of INSERT statements from `my.MessageType` protobuf messages in topic `my.Topic` filtering to only records on partition #3
```bash
kat consume my.Topic msg.TypedKafkaRecord \
  --from 1636900200000 --until 1636900800000 \
  --schema my.MessageType |\
  proto transform msg.TypedKafkaRecord binary json -f "{\"partition\":3}" \
  -t "INSERT INTO SomeTable(Id,Name) VALUES ('${msg.value.id}','${msg.value.name}')" > /tmp/script.sql
```

### Mirror a topic to another topic/cluster
Use the same keys/values produced to clusterA to populate a topic on clusterB
```bash
kat consume my.TopicA -b clusterA msg.KafkaRecord | \
  kat produce my.TopicB -b clusterB msg.KafkaRecord
```
