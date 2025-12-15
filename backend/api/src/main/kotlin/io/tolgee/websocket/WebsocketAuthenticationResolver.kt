package io.tolgee.websocket

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.security.PAT_PREFIX
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.PatService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component

@Component
class WebsocketAuthenticationResolver(
  @Lazy private val jwtService: JwtService,
  @Lazy private val apiKeyService: ApiKeyService,
  @Lazy private val patService: PatService,
  @Lazy private val userAccountService: UserAccountService,
  private val currentDateProvider: CurrentDateProvider,
) : Logging {
  /**
   * Resolves STOMP CONNECT headers into TolgeeAuthentication.
   * Supports:
   * - Authorization: Bearer <jwt>
   * - X-API-Key: tgpat_<token> (PAT) or tgpak_<...> (PAK, incl. legacy/raw)
   * - jwtToken: <jwt> (legacy header)
   *
   * It retrieves the headers from the accessor and validates them.
   */
  fun resolve(accessor: StompHeaderAccessor): TolgeeAuthentication? {
    val authorizationHeader = getCaseInsensitiveHeader(accessor, "authorization")
    val xApiKeyHeader = getCaseInsensitiveHeader(accessor, "x-api-key")
    val legacyJwtHeader = getCaseInsensitiveHeader(accessor, "jwtToken")

    // Authorization: Bearer <jwt>
    val bearer = extractBearer(authorizationHeader)
    if (bearer != null) {
      return runCatching { jwtService.validateToken(bearer) }
        .onFailure {
          logger.debug(
            "Bearer token validation failed",
            it,
          )
        }.getOrNull()
    }

    // X-API-Key: PAT / PAK
    val xApiKey = xApiKeyHeader
    if (!xApiKey.isNullOrBlank()) {
      return when {
        xApiKey.startsWith(PAT_PREFIX) ->
          runCatching { patAuth(xApiKey) }
            .onFailure {
              logger.debug(
                "PAT authentication failed",
                it,
              )
            }.getOrNull()

        else ->
          runCatching { pakAuth(xApiKey) }
            .onFailure { logger.debug("PAK authentication failed", it) }
            .getOrNull()
      }
    }

    // Legacy jwtToken header
    if (!legacyJwtHeader.isNullOrBlank()) {
      return runCatching { jwtService.validateToken(legacyJwtHeader) }
        .onFailure {
          logger.debug(
            "Legacy JWT validation failed",
            it,
          )
        }.getOrNull()
    }

    return null
  }

  private fun extractBearer(value: String?): String? {
    if (value == null) return null
    val prefix = "Bearer "
    return if (value.startsWith(prefix, ignoreCase = true)) value.substring(prefix.length).trim() else null
  }

  private fun pakAuth(key: String): TolgeeAuthentication {
    val parsed = apiKeyService.parseApiKey(key) ?: throw AuthenticationException(Message.INVALID_PROJECT_API_KEY)
    val hash = apiKeyService.hashKey(parsed)
    val pak = apiKeyService.findDto(hash) ?: throw AuthenticationException(Message.INVALID_PROJECT_API_KEY)

    if (pak.expiresAt?.before(currentDateProvider.date) == true) {
      throw AuthenticationException(Message.PROJECT_API_KEY_EXPIRED)
    }

    val userAccount: UserAccountDto =
      userAccountService.findDto(pak.userAccountId) ?: throw AuthenticationException(Message.USER_NOT_FOUND)

    apiKeyService.updateLastUsedAsync(pak.id)

    return TolgeeAuthentication(
      pak,
      deviceId = null,
      userAccount = userAccount,
      actingAsUserAccount = null,
      isReadOnly = false,
    )
  }

  private fun patAuth(key: String): TolgeeAuthentication {
    val hash = patService.hashToken(key.substring(PAT_PREFIX.length))
    val pat = patService.findDto(hash) ?: throw AuthenticationException(Message.INVALID_PAT)

    if (pat.expiresAt?.before(currentDateProvider.date) == true) {
      throw AuthenticationException(Message.PAT_EXPIRED)
    }

    val userAccount: UserAccountDto =
      userAccountService.findDto(pat.userAccountId) ?: throw AuthenticationException(Message.USER_NOT_FOUND)

    patService.updateLastUsedAsync(pat.id)

    return TolgeeAuthentication(
      credentials = pat,
      deviceId = null,
      userAccount = userAccount,
      actingAsUserAccount = null,
      isReadOnly = false,
    )
  }

  /**
   * Case-insensitive header lookup for STOMP headers.
   * Searches through message headers using case-insensitive comparison.
   */
  private fun getCaseInsensitiveHeader(
    accessor: StompHeaderAccessor,
    headerName: String,
  ): String? {
    val messageHeaders = accessor.messageHeaders
    return messageHeaders.entries
      .firstOrNull { (key, value) ->
        key.equals("nativeHeaders", ignoreCase = true) && value is Map<*, *>
      }?.let { (_, nativeHeadersMap) ->
        @Suppress("UNCHECKED_CAST")
        val nativeHeaders = nativeHeadersMap as Map<String, List<String>>
        nativeHeaders.entries
          .firstOrNull { (key, _) ->
            key.equals(headerName, ignoreCase = true)
          }?.value
          ?.firstOrNull()
      }
  }
}
