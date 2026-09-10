package io.tolgee.websocket

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.security.PAT_PREFIX
import io.tolgee.security.authentication.DisabledAuthenticationResolver
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.security.oauth2.OAuth2AccessTokenResolver
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
  @Lazy private val oauth2AccessTokenResolver: OAuth2AccessTokenResolver,
  @Lazy private val apiKeyService: ApiKeyService,
  @Lazy private val patService: PatService,
  @Lazy private val disabledAuthenticationResolver: DisabledAuthenticationResolver,
  @Lazy private val userAccountService: UserAccountService,
  private val currentDateProvider: CurrentDateProvider,
) : Logging {
  fun resolve(accessor: StompHeaderAccessor): TolgeeAuthentication? {
    return resolveCredential(accessor) ?: disabledAuthenticationResolver.resolve()
  }

  private fun resolveCredential(accessor: StompHeaderAccessor): TolgeeAuthentication? {
    val authorizationHeader = getCaseInsensitiveHeader(accessor, "authorization")
    val xApiKeyHeader = getCaseInsensitiveHeader(accessor, "x-api-key")
    val legacyJwtHeader = getCaseInsensitiveHeader(accessor, "jwtToken")

    val bearer = extractBearer(authorizationHeader)
    if (bearer != null) {
      return attempt("Bearer token") {
        oauth2AccessTokenResolver.tryResolve(bearer) ?: jwtService.validateToken(bearer)
      }
    }

    if (!xApiKeyHeader.isNullOrBlank()) {
      return when {
        xApiKeyHeader.startsWith(PAT_PREFIX) -> attempt("PAT") { patAuth(xApiKeyHeader) }
        else -> attempt("PAK") { pakAuth(xApiKeyHeader) }
      }
    }

    if (!legacyJwtHeader.isNullOrBlank()) {
      return attempt("Legacy JWT") { jwtService.validateToken(legacyJwtHeader) }
    }

    return null
  }

  private fun attempt(
    credentialKind: String,
    resolve: () -> TolgeeAuthentication?,
  ): TolgeeAuthentication? =
    runCatching(resolve)
      .onFailure { logger.debug("{} authentication failed", credentialKind, it) }
      .getOrNull()

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

  private fun getCaseInsensitiveHeader(
    accessor: StompHeaderAccessor,
    headerName: String,
  ): String? =
    accessor
      .toNativeHeaderMap()
      .entries
      .firstOrNull { it.key.equals(headerName, ignoreCase = true) }
      ?.value
      ?.firstOrNull()
}
