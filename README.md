# MSG - Monosodium Glutamate

A collection of tools for working with protobuf messages in datastores, over GRPC, and in Kafka.

## Index
- [GRPC-GUI](grpc-gui/README.md) See and type JSON, send/receive GRPC protobufs
- [Kafka-GUI](kafka-gui/README.md) Run live queries over protobufs in Kafka topics
- [Kafka-CLI](kafka-cli/README.md) Produce/consume to/from kafka via piped stdin/out
- [Proto-CLI](proto-cli/README.md) Encode/decode JSON to/from protobufs using various encodings

## Why Javascript/Node?
ProtobufJS has the amazing power to load protobuf schemas without a compilation step, so it works really well when your schemas change all the time and you don't want to have to recompile your tools.
