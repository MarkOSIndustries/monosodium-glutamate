const {loadDirectory} = require('../protobuf')
const {PROTO_HOME} = require('../env')

const messages = loadDirectory(PROTO_HOME)

module.exports = {
  decode_buffer_to_json,
  decode_buffer_to_object,
  decode_string_to_json,
  encode_json_to_buffer,
  encode_json_to_string,
  encode_object_to_buffer,
  stream_to_buffer,
  stream_to_string,
}

function decode_buffer_to_object(messageType, messageBuffer) {
  const Message = messages.lookupType(messageType);
  const messageObject = Message.decode(messageBuffer);
  return messageObject;
}

function decode_buffer_to_json(messageType, messageBuffer) {
  return JSON.stringify(decode_buffer_to_object(messageType, messageBuffer));
}

function decode_string_to_json(messageType, messageString, encoding) {
  const messageBuffer = Buffer.from(messageString, encoding);
  return decode_buffer_to_json(messageType, messageBuffer);
}

function encode_object_to_buffer(messageType, messageObject) {
  const Message = messages.lookupType(messageType);
  const messageProto = Message.fromObject(messageObject);
  return Message.encode(messageProto).finish();
}

function encode_json_to_buffer(messageType, messageJson) {
  const messageObject = JSON.parse(messageJson);
  return encode_object_to_buffer(messageType, messageObject);
}

function encode_json_to_string(messageType, messageJson, encoding) {
  return encode_json_to_buffer(messageType, messageJson).toString(encoding);
}

function stream_to_buffer(stream) {
  return new Promise((resolve,reject) => {
    const chunks = [];
    stream.on('data', function(chunk) { chunks.push(chunk); });
    stream.on('end', function() {
        const buffer = Buffer.concat(chunks);
        resolve(buffer);
    });
  });
}

function stream_to_string(stream, encoding) {
  return stream_to_buffer(stream)
    .then(buffer => buffer.toString(encoding));
}
