package msg.encodings.json

import com.alibaba.fastjson2.JSONB
import com.alibaba.fastjson2.JSONObject
import msg.encodings.Encoding
import msg.encodings.Transport

abstract class JsonEncoding<T>(
  private val underlyingEncoding: Encoding<T>,
) : Encoding<JSONObject> {
  override fun getTransport(): Transport<JSONObject> = JsonObjectTransport(this)

  fun getUnderlyingEncoding(): Encoding<T> = underlyingEncoding

  abstract fun toJsonObject(data: T): JSONObject

  abstract fun fromJsonObject(jsonObject: JSONObject): T

  class AsJson(
    underlyingEncoding: Encoding<String>,
  ) : JsonEncoding<String>(underlyingEncoding) {
    override fun toJsonObject(data: String): JSONObject = JSONObject.parse(data)

    override fun fromJsonObject(jsonObject: JSONObject): String = jsonObject.toJSONString()
  }

  class AsJsonB(
    underlyingEncoding: Encoding<ByteArray>,
  ) : JsonEncoding<ByteArray>(underlyingEncoding) {
    override fun toJsonObject(data: ByteArray): JSONObject = JSONB.parseObject(data)

    override fun fromJsonObject(jsonObject: JSONObject): ByteArray = jsonObject.toJSONBBytes()
  }
}
