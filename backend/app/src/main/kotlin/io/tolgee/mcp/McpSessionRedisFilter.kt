package io.tolgee.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpNotificationHandler
import io.modelcontextprotocol.server.McpRequestHandler
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider
import io.modelcontextprotocol.spec.DefaultMcpStreamableServerSessionFactory
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpStreamableServerSession
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Servlet filter that syncs MCP sessions to Redis for multi-replica deployments.
 *
 * The MCP Java SDK's [WebMvcStreamableServerTransportProvider] stores sessions in an in-memory
 * `ConcurrentHashMap`. When running multiple replicas behind a load balancer, requests with an
 * `Mcp-Session-Id` header may land on a replica that doesn't have the session, resulting in
 * 404 "Session not found" errors.
 *
 * This filter works around the issue by:
 * - **Before handler**: If the request carries an `Mcp-Session-Id` not present in the local map,
 *   reconstructs the session from Redis and injects it into the transport provider's sessions map.
 * - **After handler**: If the response sets a new `Mcp-Session-Id` (initialize), persists the
 *   session metadata to Redis.
 *
 * Uses reflection because [WebMvcStreamableServerTransportProvider] has a private constructor and
 * all private fields/methods — it cannot be extended or configured with custom session storage.
 *
 * When Redis is unavailable (e.g. single-instance deployment), the filter is a no-op.
 *
 * This is a temporary workaround until the SDK provides a proper session storage API.
 * Tracked upstream: https://github.com/modelcontextprotocol/java-sdk/issues/201
 * SDK maintainer confirmed persistent session storage is planned:
 * https://github.com/modelcontextprotocol/java-sdk/issues/201#issuecomment-3915069460
 */
class McpSessionRedisFilter(
  private val transportProvider: WebMvcStreamableServerTransportProvider,
  private val redissonClient: RedissonClient?,
  private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
  private val log = LoggerFactory.getLogger(McpSessionRedisFilter::class.java)

  private val sessionsMap: ConcurrentHashMap<String, McpStreamableServerSession> by lazy {
    loadSessionsMap()
  }

  private val factoryFields: FactoryFields by lazy {
    loadFactoryFields()
  }

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    if (redissonClient == null) {
      filterChain.doFilter(request, response)
      return
    }

    val sessionId = request.getHeader(MCP_SESSION_ID_HEADER)

    // Pre-handle: recover session from Redis if not in local map
    if (sessionId != null && !sessionsMap.containsKey(sessionId)) {
      recoverSessionFromRedis(sessionId)
    }

    // Wrap response to capture new session ID from initialize responses
    val responseWrapper = SessionIdCapturingResponseWrapper(response)
    filterChain.doFilter(request, responseWrapper)

    // Post-handle: persist new session to Redis
    val newSessionId = responseWrapper.capturedSessionId
    if (newSessionId != null) {
      persistSessionToRedis(newSessionId)
    }
  }

  private fun recoverSessionFromRedis(sessionId: String) {
    try {
      val bucket = redissonClient!!.getBucket<String>("$REDIS_KEY_PREFIX$sessionId")
      val json = bucket.get() ?: return

      val sessionData = objectMapper.readValue(json, McpSessionData::class.java)

      val clientCapabilities =
        sessionData.clientCapabilitiesJson?.let {
          objectMapper.readValue(it, McpSchema.ClientCapabilities::class.java)
        }
      val clientInfo =
        sessionData.clientInfoJson?.let {
          objectMapper.readValue(it, McpSchema.Implementation::class.java)
        }

      @Suppress("UNCHECKED_CAST")
      val session =
        McpStreamableServerSession(
          sessionId,
          clientCapabilities,
          clientInfo,
          factoryFields.requestTimeout,
          factoryFields.requestHandlers as Map<String, McpRequestHandler<*>>,
          factoryFields.notificationHandlers as Map<String, McpNotificationHandler>,
        )

      val existing = sessionsMap.putIfAbsent(sessionId, session)
      if (existing != null) {
        log.debug("MCP session {} was already recovered by another thread", sessionId)
      } else {
        log.debug("Recovered MCP session {} from Redis", sessionId)
      }
    } catch (e: Exception) {
      log.warn("Failed to recover MCP session {} from Redis", sessionId, e)
    }
  }

  private fun persistSessionToRedis(sessionId: String) {
    try {
      val session = sessionsMap[sessionId] ?: return

      val clientCapabilities = getPrivateField<Any?>(session, "clientCapabilities")
      val clientInfo = getPrivateField<Any?>(session, "clientInfo")

      val capabilitiesValue = unwrapField(clientCapabilities, "clientCapabilities")
      val infoValue = unwrapField(clientInfo, "clientInfo")

      val sessionData =
        McpSessionData(
          clientCapabilitiesJson = capabilitiesValue?.let { objectMapper.writeValueAsString(it) },
          clientInfoJson = infoValue?.let { objectMapper.writeValueAsString(it) },
        )

      val bucket = redissonClient!!.getBucket<String>("$REDIS_KEY_PREFIX$sessionId")
      bucket.set(objectMapper.writeValueAsString(sessionData), SESSION_TTL)
      log.debug("Persisted MCP session {} to Redis", sessionId)
    } catch (e: Exception) {
      log.warn("Failed to persist MCP session {} to Redis", sessionId, e)
    }
  }

  private fun unwrapField(
    fieldValue: Any?,
    fieldName: String,
  ): Any? {
    if (fieldValue == null) return null
    if (fieldValue is java.util.concurrent.atomic.AtomicReference<*>) {
      return fieldValue.get()
    }
    log.warn(
      "Expected AtomicReference for field '{}' but got {}; using value directly",
      fieldName,
      fieldValue.javaClass.name,
    )
    return fieldValue
  }

  @Suppress("UNCHECKED_CAST")
  private fun loadSessionsMap(): ConcurrentHashMap<String, McpStreamableServerSession> {
    val field = WebMvcStreamableServerTransportProvider::class.java.getDeclaredField("sessions")
    field.isAccessible = true
    return field.get(transportProvider) as ConcurrentHashMap<String, McpStreamableServerSession>
  }

  private fun loadFactoryFields(): FactoryFields {
    val factoryField = WebMvcStreamableServerTransportProvider::class.java.getDeclaredField("sessionFactory")
    factoryField.isAccessible = true
    val factory = factoryField.get(transportProvider)

    if (factory !is DefaultMcpStreamableServerSessionFactory) {
      throw IllegalStateException(
        "Expected DefaultMcpStreamableServerSessionFactory but got ${factory?.javaClass?.name}",
      )
    }

    val timeoutField = DefaultMcpStreamableServerSessionFactory::class.java.getDeclaredField("requestTimeout")
    timeoutField.isAccessible = true
    val requestTimeout = timeoutField.get(factory) as Duration

    val handlersField = DefaultMcpStreamableServerSessionFactory::class.java.getDeclaredField("requestHandlers")
    handlersField.isAccessible = true

    @Suppress("UNCHECKED_CAST")
    val requestHandlers = handlersField.get(factory) as Map<String, Any>

    val notifField = DefaultMcpStreamableServerSessionFactory::class.java.getDeclaredField("notificationHandlers")
    notifField.isAccessible = true

    @Suppress("UNCHECKED_CAST")
    val notificationHandlers = notifField.get(factory) as Map<String, Any>

    return FactoryFields(requestTimeout, requestHandlers, notificationHandlers)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> getPrivateField(
    obj: Any,
    fieldName: String,
  ): T {
    val field = obj.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(obj) as T
  }

  private class SessionIdCapturingResponseWrapper(
    response: HttpServletResponse,
  ) : HttpServletResponseWrapper(response) {
    var capturedSessionId: String? = null
      private set

    override fun setHeader(
      name: String,
      value: String?,
    ) {
      if (name == MCP_SESSION_ID_HEADER && value != null) {
        capturedSessionId = value
      }
      super.setHeader(name, value)
    }

    override fun addHeader(
      name: String,
      value: String?,
    ) {
      if (name == MCP_SESSION_ID_HEADER && value != null) {
        capturedSessionId = value
      }
      super.addHeader(name, value)
    }
  }

  private data class FactoryFields(
    val requestTimeout: Duration,
    val requestHandlers: Map<String, Any>,
    val notificationHandlers: Map<String, Any>,
  )

  companion object {
    private const val MCP_SESSION_ID_HEADER = "Mcp-Session-Id"
    private const val REDIS_KEY_PREFIX = "mcp_session:"
    private val SESSION_TTL = Duration.ofHours(48)
  }
}
