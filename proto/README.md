# proto
A CLI for protobuf message transformation and generation.

Can generate and translate protobuf records between several formats:
- JSON
- length-prefixed binary
- string-encoded binary with delimiters (eg: `base64` or `hex` encoding, with line delimiters)

## Usage
To clone and run this you'll need [Git](https://git-scm.com) and [Node.js v10.x](https://nodejs.org/en/download/) (which comes with [npm](http://npmjs.com)) installed on your computer.

```bash
# Clone this repository
git clone https://github.com/markosindustries/monosodium-glutamate
# Go into the repository
cd monosodium-glutamate/proto
# Install globally
npm install -g
```

After that - use from any directory containing protos:
```bash
> proto usage
> echo "{ \"thing_id\": 123, \"thing_name\": \"test\" }" | proto encode my.protobuf.package.MyMessage base64
CHsSBHRlc3Q=
```

To use `.proto`s elsewhere, set the env variable `PROTO_HOME` to the directory containing your `.proto`s.

## Why Javascript/Node?
ProtobufJS has the amazing power to load protobuf schemas without a compilation step, so it works really well when your schemas change all the time and you don't want to have to recompile your tools.
