package io.tolgee.mcp

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader

/**
 * The first bytes of a request body, read before the transport sees the request. [overCap] means the body kept going
 * past the cap and [bytes] is only a prefix of it, so nothing should be decided from the content.
 */
class PeekedRequestBody private constructor(
  private val request: HttpServletRequest,
  val bytes: ByteArray,
  val overCap: Boolean,
) {
  /** A request that serves the peeked body again, afresh on every stream or reader access. */
  fun replay(): HttpServletRequest {
    check(!overCap) { "the body ran past the cap, so only a prefix of it was read; it cannot be replayed" }
    return BufferedReplayRequest(request, bytes)
  }

  companion object {
    fun peek(
      request: HttpServletRequest,
      cap: Int,
    ): PeekedRequestBody {
      // Read one byte past the cap so an exactly-cap body still fits while a larger one is detectably over.
      val prefix = ByteArray(cap + 1)
      val read = request.inputStream.readNBytes(prefix, 0, prefix.size)
      return PeekedRequestBody(request, prefix.copyOf(minOf(read, cap)), overCap = read > cap)
    }
  }
}

/**
 * Re-serves a fully-read body through both [getInputStream] and [getReader], afresh on every call — the transport
 * reads the stream, but an unoverridden [getReader] would hand it a drained one.
 */
private class BufferedReplayRequest(
  request: HttpServletRequest,
  private val buffered: ByteArray,
) : HttpServletRequestWrapper(request) {
  private val charset get() = characterEncoding ?: Charsets.UTF_8.name()

  override fun getInputStream(): ServletInputStream = ReplayServletInputStream(ByteArrayInputStream(buffered))

  override fun getReader(): BufferedReader = BufferedReader(InputStreamReader(ByteArrayInputStream(buffered), charset))
}

private class ReplayServletInputStream(
  private val delegate: InputStream,
) : ServletInputStream() {
  override fun read(): Int = delegate.read()

  override fun read(
    b: ByteArray,
    off: Int,
    len: Int,
  ): Int = delegate.read(b, off, len)

  override fun isFinished(): Boolean = delegate.available() == 0

  override fun isReady(): Boolean = true

  override fun setReadListener(listener: ReadListener?) {
    throw UnsupportedOperationException()
  }
}
