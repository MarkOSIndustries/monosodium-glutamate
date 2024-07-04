# proto
A CLI for protobuf message transformation and generation.

Can generate and translate protobuf records between several formats:
- JSON
- length-prefixed binary
- string-encoded binary with delimiters (eg: `base64` or `hex` encoding, with line delimiters)

## To Use
To clone and run this you'll need [Git](https://git-scm.com), [Java 11](http://openjdk.java.net/install/), and [Gradle](https://gradle.org/install/) installed on your computer.

```bash
# Clone this repository
git clone https://github.com/markosindustries/monosodium-glutamate
# Go into the repository
cd monosodium-glutamate/proto
# Install dependencies
gradle build
# Run the app
java -jar build/install/proto/proto*.jar
```

Example usage:
```bash
> echo "{ \"thing_id\": 123, \"thing_name\": \"test\" }" | proto transform my.protobuf.package.MyMessage json base64
CHsSBHRlc3Q=
```

To use `.proto`s elsewhere, set the env variable `PROTO_HOME` to the directory containing your `.proto`s.

If you have multiple - you can comma separate all of your `.proto` root folders

## Full list of transform encodings

All transform options effect the behaviour of this pipeline:
```
stdin -> unmarshal -> filter -> shape -> marshal -> stdout
```
The goal is to always have data in a DynamicMessage as it enters the **filter** and **shape** steps.

Here's what each encoding means in that context.

# Unmarshal
|        Encoding | Read | Decode | Deserialise | JSON |
| ---------------:|:------ |
|        **json** | read delimited strings | | | parse as json |
|         **hex** | read delimited strings | hex to bytes | deserialise as protobuf schema | |
|      **base64** | read delimited strings | base64 to bytes | deserialise as protobuf schema | |
|      **binary** | read length-prefixed byte arrays | | deserialise as protobuf schema | |
|    **json_hex** | read delimited strings | hex to bytes | | parse as json |
| **json_base64** | read delimited strings | base64 to bytes | | parse as json |

# Marshal
|    Encoding | JSON | Serialise | Encode | Write |
| -----------:|:------ |
|        json | stringify json  | | | write delimited strings |
|         hex | | serialise protobuf to bytes | bytes to hex | write delimited strings |
|      base64 | | serialise protobuf to bytes | bytes to base64 | write delimited strings |
|      binary | | serialise protobuf to bytes | | write length-prefixed byte arrays |
|    json_hex | stringify json | | bytes to hex | write delimited strings |
| json_base64 | stringify json | | bytes to base64 | write delimited strings |
