package io.tolgee.ee.component.slackIntegration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URLDecoder
import java.util.Collections
import java.util.Enumeration

/**
 * Slack signs the exact bytes it sends. Spring rebuilds a form-encoded `@RequestBody` from the parsed
 * parameters with its own percent-encoding (e.g. Slack's `%2A` comes back as `*`), so the signature must be
 * checked against the body captured here, before anything parses the parameters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SlackRawBodyFilter : OncePerRequestFilter() {
  companion object {
    const val RAW_BODY_ATTRIBUTE = "slackRawBody"
    private const val MAX_BODY_BYTES = 1024 * 1024
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean =
    !request.requestURI.removePrefix(request.contextPath).startsWith("/v2/public/slack")

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    // Runs before the rate limiter, so the body must stay bounded like Tomcat's maxPostSize used to keep it.
    val bytes = request.inputStream.readNBytes(MAX_BODY_BYTES + 1)
    if (bytes.size > MAX_BODY_BYTES) {
      response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE)
      return
    }
    val rawBody = bytes.toString(Charsets.UTF_8)
    val wrapped =
      try {
        RawBodyFormRequest(request, rawBody)
      } catch (e: IllegalArgumentException) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST)
        return
      }
    request.setAttribute(RAW_BODY_ATTRIBUTE, rawBody)
    filterChain.doFilter(wrapped, response)
  }

  private class RawBodyFormRequest(
    request: HttpServletRequest,
    rawBody: String,
  ) : HttpServletRequestWrapper(request) {
    private val parameters: Map<String, Array<String>> =
      (parseForm(request.queryString) + parseFormBody(request, rawBody))
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.toTypedArray() }

    override fun getParameter(name: String): String? = parameters[name]?.firstOrNull()

    override fun getParameterMap(): Map<String, Array<String>> = parameters

    override fun getParameterNames(): Enumeration<String> = Collections.enumeration(parameters.keys)

    override fun getParameterValues(name: String): Array<String>? = parameters[name]

    private fun parseFormBody(
      request: HttpServletRequest,
      rawBody: String,
    ): List<Pair<String, String>> {
      val isForm = request.contentType?.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE, ignoreCase = true)
      if (isForm != true) return emptyList()
      return parseForm(rawBody)
    }

    private fun parseForm(encoded: String?): List<Pair<String, String>> =
      encoded.orEmpty().split('&').filter { it.isNotEmpty() }.map {
        URLDecoder.decode(it.substringBefore('='), Charsets.UTF_8) to
          URLDecoder.decode(it.substringAfter('=', ""), Charsets.UTF_8)
      }
  }
}
