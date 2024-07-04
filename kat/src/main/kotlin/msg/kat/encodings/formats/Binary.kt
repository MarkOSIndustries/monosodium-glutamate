package msg.kat.encodings.formats

import msg.encodings.Transport
import msg.kat.encodings.KafkaEncoding

class Binary(transport: Transport<ByteArray>) : KafkaEncoding.OfBinary(transport)
