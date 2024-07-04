package msg.proto.encodings.formats

import msg.encodings.Transport
import msg.proto.encodings.ProtobufEncoding

class Binary(transport: Transport<ByteArray>) : ProtobufEncoding.OfBinary(transport)
