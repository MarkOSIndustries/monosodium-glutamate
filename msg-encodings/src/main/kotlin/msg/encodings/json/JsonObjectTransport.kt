package msg.encodings.json

import com.alibaba.fastjson2.JSONObject
import msg.encodings.Transport
import java.io.InputStream
import java.io.PrintStream

class JsonObjectTransport<T>(
  private val jsonEncoding: JsonEncoding<T>,
) : Transport<JSONObject> {
  override fun reader(input: InputStream): Iterator<JSONObject> = JsonObjectIterator(jsonEncoding, input)

  override fun writer(output: PrintStream): (JSONObject) -> Unit {
    val write = jsonEncoding.getUnderlyingEncoding().getTransport().writer(output)
    return { write(jsonEncoding.fromJsonObject(it)) }
  }

  private class JsonObjectIterator<T>(
    private val jsonEncoding: JsonEncoding<T>,
    input: InputStream,
  ) : Iterator<JSONObject> {
    private val reader = jsonEncoding.getUnderlyingEncoding().getTransport().reader(input)

    override fun hasNext(): Boolean = reader.hasNext()

    override fun next(): JSONObject = jsonEncoding.toJsonObject(reader.next())
  }
}
