package msg.encodings.formats

import msg.encodings.Encoding
import msg.encodings.Transport

class Binary(
  transport: Transport<ByteArray>,
) : Encoding.OfBinary(transport)
