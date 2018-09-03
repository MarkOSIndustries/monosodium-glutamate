const impl = require('./implementations');

module.exports = actions = {
  encode,
  decode,
  encode_stdin,
  decode_stdin,
  usage,
};

const INSTALL_NAME = "proto-cli";
const DEFAULT_ENCODING = process.env.PROTO_CLI_DEFAULT_ENCODING || 'hex';
const VALID_ENCODINGS = ['binary', 'base64', 'hex', 'ascii', 'utf8', 'utf16le'];
const isBinary = encoding => encoding === 'binary';

function usage() {
  console.log(`Usage: ${INSTALL_NAME} <action> [...options]`);
  console.log(`Valid actions:\n  ${Object.keys(actions).join('\n  ')}`);
  return;
}

function encode(messageType, json, encoding) {
  encoding = encoding || DEFAULT_ENCODING;
  if(arguments.length < 2) {
    console.log(`Usage: ${INSTALL_NAME} encode <messageType> <json> [encoding]`);
    console.log(`  messageType: The fully specified message definition (eg: somepackage.SomeMessage)`);
    console.log(`         json: The JSON payload to encode.`);
    console.log(`     encoding: [Optional,default=${DEFAULT_ENCODING}] the output encoding.`);
    console.log(`               eg: ${VALID_ENCODINGS.join(', ')}`);
    return;
  }
  
  const buffer = impl.encode_json_to_buffer(messageType, json);
  process.stdout.write(isBinary(encoding) ? buffer : buffer.toString(encoding));
}

function decode(messageType, encodedString, encoding) {
  encoding = encoding || DEFAULT_ENCODING;
  if(arguments.length < 2) {
    console.log(`Usage: ${INSTALL_NAME} decode <messageType> <encodedString> [...options]`);
    console.log(`  messageType: The fully specified message definition (eg: somepackage.SomeMessage)`);
    console.log(` encodedString: The message data to decode.`);
    console.log(`     encoding: [Optional,default=${DEFAULT_ENCODING}] the input encoding.`);
    console.log(`               eg: ${VALID_ENCODINGS.join(', ')}`);
    return;
  }
  if(isBinary(encoding)) {
    console.log("Action 'decode' does not support 'binary' encoding. Try decode_stdin for this");
    return;
  }
  
  const json = impl.decode_string_to_json(messageType, encodedString, encoding);
  
  process.stdout.write(json);
}

function encode_stdin(messageType, encoding) {
  encoding = encoding || DEFAULT_ENCODING;
  if(arguments.length < 1) {
    console.log('Same as encode but reads JSON from stdin');
    console.log(`Usage: ${INSTALL_NAME} encode_stdin <messageType> [encoding]`);
    console.log(`  messageType: The fully specified message definition (eg: somepackage.SomeMessage)`);
    console.log(`     encoding: [Optional,default=${DEFAULT_ENCODING}] the output encoding.`);
    console.log(`               eg: ${VALID_ENCODINGS.join(', ')}`);
    return;
  }
  
  impl
    .stream_to_string(process.stdin, 'utf8')
    .then(json => {
      const buffer = impl.encode_json_to_buffer(messageType, json);
      process.stdout.write(isBinary(encoding) ? buffer : buffer.toString(encoding));
    });
}

function decode_stdin(messageType, encoding) {
  encoding = encoding || DEFAULT_ENCODING;
  if(arguments.length < 1) {
    console.log('Same as decode but reads encoded string from stdin');
    console.log(`Usage: ${INSTALL_NAME} decode_stdin <messageType> [encoding]`);
    console.log(`  messageType: The fully specified message definition (eg: somepackage.SomeMessage)`);
    console.log(`     encoding: [Optional,default=${DEFAULT_ENCODING}] the input encoding.`);
    console.log(`               eg: ${VALID_ENCODINGS.join(', ')}`);
    return;
  }
  
  impl
    .stream_to_buffer(process.stdin)
    .then(buffer => {
      const json = impl.decode_buffer_to_json(messageType, isBinary(encoding) ? buffer : Buffer.from(buffer.toString('ascii'), encoding));
      process.stdout.write(json);
    });
}