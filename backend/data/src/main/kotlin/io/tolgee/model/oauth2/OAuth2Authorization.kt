package io.tolgee.model.oauth2

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.security.oauth2.OAuth2Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import java.util.Date

/**
 * One OAuth 2.1 authorization: a user granting a client a scope set on a project set. Created when the client hits
 * `/oauth2/authorize`, bound on the consent screen, then carries the authorization code and the tokens it produced.
 *
 * Codes and tokens are stored hashed, like project API keys and PATs; the plaintext exists only in the response that
 * delivered it.
 */
@Entity
@Table(
  name = "oauth2_authorization",
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["consent_state"], name = "oauth2_authorization_consent_state_unique"),
    UniqueConstraint(columnNames = ["code_hash"], name = "oauth2_authorization_code_hash_unique"),
    UniqueConstraint(columnNames = ["access_token_hash"], name = "oauth2_authorization_access_token_hash_unique"),
    UniqueConstraint(columnNames = ["refresh_token_hash"], name = "oauth2_authorization_refresh_token_hash_unique"),
  ],
  indexes = [
    Index(columnList = "user_account_id"),
  ],
)
class OAuth2Authorization : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  @NotNull
  lateinit var userAccount: UserAccount

  @Column(nullable = false)
  @NotNull
  var clientId: String = ""

  @Column(nullable = false, length = 2000)
  @NotNull
  var redirectUri: String = ""

  @Column(length = 2000)
  var clientState: String? = null

  @Column(nullable = false)
  @NotNull
  var codeChallenge: String = ""

  /** Space-delimited scope values the client asked for on `/oauth2/authorize`. */
  @Column(nullable = false, length = 4000)
  @NotNull
  var requestedScopes: String = ""

  /** Space-delimited scope values the user approved; null until consent. */
  @Column(length = 4000)
  var grantedScopes: String? = null

  /** The client's `project` hint on the authorize request, whatever the user later chose. */
  var projectHint: Long? = null

  /**
   * What the consent screen bound the token to: [OAuth2Constants.ALL_PROJECTS], or comma-separated project ids.
   * Null until the screen made the choice.
   */
  var projectSelection: String? = null

  /** Keys the pending authorization for the consent screen; distinct from the client's own [clientState]. */
  var consentState: String? = null

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

  @Temporal(TemporalType.TIMESTAMP)
  var refreshTokenExpiresAt: Date? = null

  var requestedScopeValues: List<String>
    get() = splitScopes(requestedScopes)
    set(value) {
      requestedScopes = value.joinToString(" ")
    }

  var grantedScopeValues: List<String>
    get() = splitScopes(grantedScopes ?: "")
    set(value) {
      grantedScopes = value.joinToString(" ")
    }

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

  private fun splitScopes(raw: String): List<String> = raw.split(" ").filter { it.isNotBlank() }
}
