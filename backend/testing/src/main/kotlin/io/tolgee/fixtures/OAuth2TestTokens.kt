package io.tolgee.fixtures

import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.OAuth2Constants
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Issues opaque OAuth2 access tokens straight into the authorization store, so a test can exercise the resolver without
 * driving the whole authorization-code dance.
 *
 * The token is only ever resolvable through `oauth2_authorization`, so the row — and a registered client for it to
 * point at — must exist. [deleteAll] cleans up what a test issued.
 */
class OAuth2TestTokens(
  private val authorizationService: OAuth2AuthorizationService,
  private val registeredClientRepository: RegisteredClientRepository,
) {
  private val issuedAuthorizationIds = mutableListOf<String>()

  fun registerClient(clientId: String): RegisteredClient {
    registeredClientRepository.findByClientId(clientId)?.let { return it }
    val client =
      RegisteredClient
        .withId(clientId)
        .clientId(clientId)
        .clientName(clientId)
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://example.org/callback")
        .apply { Scope.entries.forEach { scope(it.value) } }
        .clientSettings(ClientSettings.builder().requireProofKey(true).build())
        .build()
    registeredClientRepository.save(client)
    return client
  }

  /**
   * @param projects project ids the token is bound to, or [OAuth2Constants.ALL_PROJECTS] for the not-narrowed sentinel.
   *   Ids are stamped as strings, matching what the token customizer writes.
   */
  fun issue(
    subject: Long,
    scopes: List<String>,
    projects: Any = OAuth2Constants.ALL_PROJECTS,
    clientId: String = DEFAULT_CLIENT_ID,
    issuedAt: Instant = Instant.now(),
    expiresAt: Instant = issuedAt.plus(Duration.ofMinutes(30)),
  ): String {
    val client = registerClient(clientId)
    val value = "test-" + UUID.randomUUID().toString().replace("-", "")
    val accessToken = OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, value, issuedAt, expiresAt, scopes.toSet())
    val authorization =
      OAuth2Authorization
        .withRegisteredClient(client)
        .id(UUID.randomUUID().toString())
        .principalName(subject.toString())
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizedScopes(scopes.toSet())
        .token(accessToken) { metadata ->
          metadata[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] =
            mapOf(
              "sub" to subject.toString(),
              "jti" to UUID.randomUUID().toString(),
              OAuth2Constants.PROJECTS_CLAIM to normalizeProjects(projects),
            )
        }.build()
    authorizationService.save(authorization)
    issuedAuthorizationIds.add(authorization.id)
    return value
  }

  /**
   * SAS's JDBC store deserializes stored claims through an allowlisting `PolymorphicTypeValidator` that rejects
   * `java.lang.Long`, so a numeric project id round-trips only as a String — which is what the production token
   * customizer writes. Callers may pass ids as numbers; they are stamped the same way here.
   */
  private fun normalizeProjects(projects: Any): Any {
    if (projects is Collection<*>) return projects.map { it.toString() }
    return projects
  }

  fun revoke(token: String) {
    authorizationService
      .findByToken(token, OAuth2TokenType.ACCESS_TOKEN)
      ?.let { authorizationService.remove(it) }
  }

  fun deleteAll() {
    issuedAuthorizationIds.forEach { id ->
      authorizationService.findById(id)?.let { authorizationService.remove(it) }
    }
    issuedAuthorizationIds.clear()
  }

  companion object {
    const val DEFAULT_CLIENT_ID = "test-oauth-client"
  }
}
