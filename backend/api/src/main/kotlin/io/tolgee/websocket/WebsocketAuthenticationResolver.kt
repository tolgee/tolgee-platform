package io.tolgee.websocket

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.security.PAT_PREFIX
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.PatService
import io.tolgee.service.security.UserAccountService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class WebsocketAuthenticationResolver(
  @Lazy private val jwtService: JwtService,
  @Lazy private val apiKeyService: ApiKeyService,
  @Lazy private val patService: PatService,
  @Lazy private val userAccountService: UserAccountService,
) {
  /**
   * Resolves STOMP CONNECT headers into TolgeeAuthentication.
   * Supports:
   * - Authorization: Bearer <jwt>
   * - X-API-Key: tgpat_<token> (PAT) or tgpak_<...> (PAK, incl. legacy/raw)
   * - jwtToken: <jwt> (legacy header)
   */
  fun resolve(
    authorizationHeader: String?,
    xApiKeyHeader: String?,
    legacyJwtHeader: String?,
  ): TolgeeAuthentication? {
    // Authorization: Bearer <jwt>
    val bearer = extractBearer(authorizationHeader)
    if (bearer != null) {
      return runCatching { jwtService.validateToken(bearer) }.getOrNull()
    }

    // X-API-Key: PAT / PAK
    val xApiKey = xApiKeyHeader
    if (!xApiKey.isNullOrBlank()) {
      return when {
        xApiKey.startsWith(PAT_PREFIX) -> runCatching { patAuth(xApiKey) }.getOrNull()
        else -> runCatching { pakAuth(xApiKey) }.getOrNull()
      }
    }

    // Legacy jwtToken header
    if (!legacyJwtHeader.isNullOrBlank()) {
      return runCatching { jwtService.validateToken(legacyJwtHeader) }.getOrNull()
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

    if (pak.expiresAt?.before(java.util.Date()) == true) {
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

    if (pat.expiresAt?.before(java.util.Date()) == true) {
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
}
