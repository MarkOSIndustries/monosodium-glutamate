function initWithVarInt(varint) {
  const varIntLengthPrefixReader = {
    tryReadPrefix(buffer, lengthCallback) {
      let length
      try {
        length = varint.decode(buffer)
      } catch(ex) {
        return false
      }
      lengthCallback(length, varint.decode.bytes)
      return true
    },
  }

  const varIntLengthPrefixWriter = {
    prefixSize(length) {
      return varint.encodingLength(length)
    },
    writePrefix(buffer, length) {
      Buffer.from(varint.encode(length)).copy(buffer)
    },
  }

  return {
    varIntLengthPrefixReader,
    varIntLengthPrefixWriter,
  }
}

module.exports = initWithVarInt