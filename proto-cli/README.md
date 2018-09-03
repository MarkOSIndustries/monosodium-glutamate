# proto-cli
A CLI for protobuf message transformation.

Useful when you put protobufs into various systems using various encodings (eg: `base64` and `hex`)

## Usage
Clone this repo, then
```bash
npm install -g
```

After that - use from any directory containing protos:
```bash
proto-cli usage
proto-cli encode MyProtoMessage {\"field\":\"value\"} base64
```

To use `.proto`s elsewhere, set the env variable `PROTO_CLI_HOME` to the directory containing your `.proto`s.
