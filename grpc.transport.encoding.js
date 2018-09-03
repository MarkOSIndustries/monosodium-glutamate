const zlib = require('zlib')

const IdentityEncoding = {
  compressed: 0,
  name: 'identity',
  encode: (buffer) => Promise.resolve(buffer),
  decode: (buffer) => Promise.resolve(buffer),
}

const GZIPEncoding = {
  compressed: 1,
  name: 'gzip',
  encode: (buffer) => new Promise((resolve,reject) => {
    zlib.gzip(buffer, (err, encodedBuffer) => {
      if(err) {
        reject(err)
      } else {
        resolve(encodedBuffer)
      }
    })
  }),
  decode: (buffer) => new Promise((resolve,reject) => {
    zlib.gunzip(buffer, (err, decodedBuffer) => {
      if(err) {
        reject(err)
      } else {
        resolve(decodedBuffer)
      }
    })
  })
}

const DeflateEncoding = {
  compressed: 1,
  name: 'deflate',
  encode: (buffer) => new Promise((resolve,reject) => {
    zlib.deflate(buffer, (err, encodedBuffer) => {
      if(err) {
        reject(err)
      } else {
        resolve(encodedBuffer)
      }
    })
  }),
  decode: (buffer) => new Promise((resolve,reject) => {
    zlib.inflate(buffer, (err, decodedBuffer) => {
      if(err) {
        reject(err)
      } else {
        resolve(decodedBuffer)
      }
    })
  })
}

const SnappyEncoding = {
  compressed: 1,
  name: 'snappy',
  encode: (buffer) => Promise.reject(new Error('Snappy encoding is not yet supported')),
  decode: (buffer) => Promise.reject(new Error('Snappy decoding is not yet supported')),
}

const GRPCEncodingsByName = {
  [IdentityEncoding.name]: IdentityEncoding,
  [GZIPEncoding.name]: GZIPEncoding,
  [DeflateEncoding.name]: DeflateEncoding,
  [SnappyEncoding.name]: SnappyEncoding,
}

module.exports = {
  IdentityEncoding,
  GZIPEncoding,
  DeflateEncoding,
  SnappyEncoding,

  GRPCEncodingsByName,
}
