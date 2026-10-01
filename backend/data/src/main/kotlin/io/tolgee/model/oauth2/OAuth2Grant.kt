package io.tolgee.model.oauth2

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.OAuth2Audience
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.security.oauth2.OAuth2Scopes
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * One OAuth 2.1 authorization: a user granting a client a scope set on a project set. Created when the client hits
 * `/oauth2/authorize`, bound on the consent screen, then carries the authorization code and the tokens it produced.
 *
 * Codes and tokens are stored hashed, like project API keys and PATs; the plaintext exists only in the response that
 * delivered it.
 */
@Entity
// Not `oauth2_authorization`: that is Spring Authorization Server's default table name, and Tolgee may share a schema
// with an application that owns those rows.
@Table(
  name = "oauth2_grant",
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["consent_state"], name = "oauth2_grant_consent_state_unique"),
    UniqueConstraint(columnNames = ["code_hash"], name = "oauth2_grant_code_hash_unique"),
    UniqueConstraint(columnNames = ["access_token_hash"], name = "oauth2_grant_access_token_hash_unique"),
    UniqueConstraint(columnNames = ["refresh_token_hash"], name = "oauth2_grant_refresh_token_hash_unique"),
  ],
  indexes = [
    Index(columnList = "user_account_id"),
    Index(columnList = "previous_refresh_token_hash"),
  ],
)
class OAuth2Grant : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var userAccount: UserAccount

  @Column(nullable = false)
  var clientId: String = ""

  @Column(length = 2000, nullable = false)
  var redirectUri: String = ""

  @Column(length = 2000)
  var clientState: String? = null

  @Column(nullable = false)
  var codeChallenge: String = ""

  @Column(length = 4000, nullable = false)
  var requestedScopes: String = ""

  @Column(length = 16, nullable = false)
  var audience: String = OAuth2Audience.API.name

  @Column(length = 255)
  var clientMetadataHash: String? = null

  @Column(length = 4000)
  var maxGrantedScopes: String? = null

  @Column(length = 4000)
  var issuedTokenScopes: String? = null

  var projectHint: Long? = null

  /**
   * What the consent screen bound the token to: [OAuth2Constants.ALL_PROJECTS], or comma-separated project ids.
   * Null until the screen made the choice.
   */
  @Column(length = 2000)
  var projectSelection: String? = null

  var consentState: String? = null

  @Temporal(TemporalType.TIMESTAMP)
  var consentExpiresAt: Date? = null

  var codeHash: String? = null

  @Temporal(TemporalType.TIMESTAMP)
  var codeExpiresAt: Date? = null

  @Temporal(TemporalType.TIMESTAMP)
  var codeUsedAt: Date? = null

  var accessTokenHash: String? = null

  @Temporal(TemporalType.TIMESTAMP)
  var accessTokenIssuedAt: Date? = null

  @Temporal(TemporalType.TIMESTAMP)
  var accessTokenExpiresAt: Date? = null

  var refreshTokenHash: String? = null

  /** The refresh token this grant's current one replaced, so a replay can be told from a guess. */
  var previousRefreshTokenHash: String? = null

  @Temporal(TemporalType.TIMESTAMP)
  var refreshTokenRotatedAt: Date? = null

  @Temporal(TemporalType.TIMESTAMP)
  var refreshTokenExpiresAt: Date? = null

  /**
   * When the client's metadata document was last seen to refuse - taken down, or no longer valid. Cleared when the
   * document resolves again, so a publisher's outage is recoverable.
   */
  @Temporal(TemporalType.TIMESTAMP)
  var clientWithdrawnAt: Date? = null

  /**
   * When this client's metadata document was last read and accepted, for a client that has one. Null means it has
   * not been read since the grant was made, so [io.tolgee.model.StandardAuditModel.createdAt] is the age to use.
   */
  @Temporal(TemporalType.TIMESTAMP)
  var cimdVerifiedAt: Date? = null

  /** Removing the cascade breaks the theft path's flush; the FK's own cascade covers the bulk JPQL reaper instead. */
  @OneToMany(mappedBy = "grant", cascade = [CascadeType.ALL], orphanRemoval = true)
  var supersededRefreshTokens: MutableList<OAuth2SupersededRefreshToken> = mutableListOf()

  fun boundAudience(): OAuth2Audience? = OAuth2Audience.entries.firstOrNull { it.name == audience }

  fun bindAudience(value: OAuth2Audience) {
    audience = value.name
  }

  var requestedScopeValues: List<String>
    get() = wireValuesOf(requestedScopes)
    set(value) {
      requestedScopes = storedNamesOf(value)
    }

  var maxGrantedScopeValues: List<String>
    get() = wireValuesOf(maxGrantedScopes)
    set(value) {
      maxGrantedScopes = storedNamesOf(value)
    }

  var issuedTokenScopeValues: List<String>
    get() = wireValuesOf(issuedTokenScopes)
    set(value) {
      issuedTokenScopes = storedNamesOf(value)
    }

  fun issuedTokenScopeSet(): Set<Scope> = storedScopesOf(issuedTokenScopes).toSet()

  /**
   * Project ids the authorization is bound to, or null for [OAuth2Constants.ALL_PROJECTS].
   *
   * Only the literal [OAuth2Constants.ALL_PROJECTS] sentinel ever means "every project": nothing selected yields an
   * empty set, and an id that does not parse is dropped, both of which narrow the binding.
   */
  fun boundProjectIds(): Set<Long>? {
    val selection = projectSelection ?: return emptySet()
    if (selection == OAuth2Constants.ALL_PROJECTS) return null
    return selection.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
  }

  fun bindProjects(projectIds: Collection<Long>?) {
    projectSelection = projectIds?.joinToString(",") ?: OAuth2Constants.ALL_PROJECTS
  }

  /**
   * Scopes are stored as [Scope] *names* and exposed as wire values. A name that no longer resolves is dropped here,
   * which narrows the grant rather than failing it or honouring a scope the codebase no longer defines.
   */
  private fun storedScopesOf(stored: String?): List<Scope> =
    OAuth2Scopes.splitScopeString(stored).mapNotNull { OAuth2Scopes.findByName(it) }

  private fun wireValuesOf(stored: String?): List<String> = storedScopesOf(stored).map { it.value }

  private fun storedNamesOf(wireValues: List<String>): String =
    wireValues.mapNotNull { OAuth2Scopes.find(it)?.name }.joinToString(" ")
}
