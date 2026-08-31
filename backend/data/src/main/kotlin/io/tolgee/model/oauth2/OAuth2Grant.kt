package io.tolgee.model.oauth2

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.security.oauth2.OAuth2Scopes
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
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

  /** The ceiling a refresh can never exceed; null until consent. */
  @Column(length = 4000)
  var grantedScopes: String? = null

  /**
   * What the *currently issued* token carries — [grantedScopes], or a subset of it when a refresh asked for less.
   * Kept apart from [grantedScopes] so a narrowing refresh does not shrink the grant itself.
   */
  @Column(length = 4000)
  var activeScopes: String? = null

  var projectHint: Long? = null

  /**
   * What the consent screen bound the token to: [OAuth2Constants.ALL_PROJECTS], or comma-separated project ids.
   * Null until the screen made the choice.
   */
  @Column(length = 2000)
  var projectSelection: String? = null

  /**
   * Keys the pending authorization for the consent screen; distinct from the client's own [clientState]. Cleared when
   * consent resolves the authorization, so a spent grant can no longer be addressed by it.
   */
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
  var refreshTokenExpiresAt: Date? = null

  var requestedScopeValues: List<String>
    get() = wireValuesOf(requestedScopes)
    set(value) {
      requestedScopes = storedNamesOf(value)
    }

  var grantedScopeValues: List<String>
    get() = wireValuesOf(grantedScopes)
    set(value) {
      grantedScopes = storedNamesOf(value)
    }

  var activeScopeValues: List<String>
    get() = wireValuesOf(activeScopes)
    set(value) {
      activeScopes = storedNamesOf(value)
    }

  fun activeScopeSet(): Set<Scope> = storedScopesOf(activeScopes).toSet()

  /**
   * Project ids the authorization is bound to, or null for [OAuth2Constants.ALL_PROJECTS].
   *
   * Nothing selected, or a value that doesn't parse, must not read as "all projects": it yields an empty set, so the
   * token reaches no project at all.
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
