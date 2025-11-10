package io.tolgee.model

import io.hypersistence.utils.hibernate.type.array.ListArrayType
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.api.IUserAccount
import io.tolgee.component.ThirdPartyAuthTypeConverter
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackUserConnection
import io.tolgee.model.task.Task
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type
import java.util.Date

@Entity
@ActivityLoggedEntity
data class UserAccount(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  override var id: Long = 0L,
  @field:NotBlank
  override var username: String = "",
  var password: String? = null,
  var name: String = "",
  @Enumerated(EnumType.STRING)
  var role: Role? = Role.USER,
  /**
   * This property is redundant - it's value can be derived from other existing properties.
   * Kept for legacy reasons.
   *
   * It's value follows these rules, but there are some edge cases related to old accounts:
   * - NATIVE ->      password != null && thirdPartyAuthType == GITHUB | GOOGLE | OAUTH | null
   * - THIRD_PARTY -> password == null && thirdPartyAuthType == GITHUB | GOOGLE | OAUTH
   * - MANAGED ->     password == null && thirdPartyAuthType == SSO | SSO_GLOBAL
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "account_type")
  override var accountType: AccountType? = AccountType.LOCAL,
) : AuditModel(),
  ModelWithAvatar,
  IUserAccount,
  EntityWithId {
  @Column(name = "totp_key", columnDefinition = "bytea")
  override var totpKey: ByteArray? = null

  @Type(ListArrayType::class)
  @Column(name = "mfa_recovery_codes", columnDefinition = "text[]")
  var mfaRecoveryCodes: List<String> = emptyList()

  @Column(name = "tokens_valid_not_before")
  var tokensValidNotBefore: Date? = null

  @OneToMany(mappedBy = "user", orphanRemoval = true)
  var permissions: MutableSet<Permission> = mutableSetOf()

  @OneToOne(mappedBy = "userAccount", fetch = FetchType.LAZY, optional = true)
  var emailVerification: EmailVerification? = null

  @OneToOne(
    mappedBy = "userAccount",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.REMOVE],
    orphanRemoval = true,
    optional = true,
  )
  var authProviderChangeRequest: AuthProviderChangeRequest? = null

  @Column(name = "third_party_auth_type")
  @Convert(converter = ThirdPartyAuthTypeConverter::class)
  var thirdPartyAuthType: ThirdPartyAuthType? = null

  @Column(name = "sso_refresh_token", columnDefinition = "TEXT")
  var ssoRefreshToken: String? = null

  @Column(name = "sso_session_expiry")
  var ssoSessionExpiry: Date? = null

  @Column(name = "third_party_auth_id")
  var thirdPartyAuthId: String? = null

  @Column(name = "reset_password_code")
  var resetPasswordCode: String? = null

  @OrderBy("id ASC")
  @OneToMany(mappedBy = "user", orphanRemoval = true)
  var organizationRoles: MutableList<OrganizationRole> = mutableListOf()

  @OneToOne(mappedBy = "userAccount", fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], orphanRemoval = true)
  var preferences: UserPreferences? = null

  @OneToMany(mappedBy = "userAccount", orphanRemoval = true)
  var pats: MutableList<Pat>? = mutableListOf()

  @OneToMany(mappedBy = "userAccount", orphanRemoval = true)
  var apiKeys: MutableList<ApiKey>? = mutableListOf()

  @OneToMany(mappedBy = "createdBy", orphanRemoval = true)
  var invitations: MutableList<Invitation>? = mutableListOf()

  override var avatarHash: String? = null

  @Column(name = "deleted_at")
  var deletedAt: Date? = null

  @Column(name = "disabled_at")
  var disabledAt: Date? = null

  @Column(name = "is_initial_user", nullable = false)
  @ColumnDefault("false")
  override var isInitialUser: Boolean = false

  @Column(name = "password_changed", nullable = false)
  @ColumnDefault("true")
  var passwordChanged: Boolean = true

  /**
   * Whether user is created only to be used as a part of demo data
   */
  @ColumnDefault("false")
  var isDemo: Boolean = false

  @OneToMany(mappedBy = "userAccount", fetch = FetchType.LAZY, orphanRemoval = true)
  var slackUserConnection: MutableList<SlackUserConnection> = mutableListOf()

  @OneToMany(mappedBy = "userAccount", fetch = FetchType.LAZY, orphanRemoval = true)
  var slackConfig: MutableList<SlackConfig> = mutableListOf()

  @ManyToMany(mappedBy = "assignees")
  var tasks: MutableSet<Task> = mutableSetOf()

  constructor(
    id: Long?,
    username: String?,
    password: String?,
    name: String?,
    permissions: MutableSet<Permission>,
    role: Role = Role.USER,
    accountType: AccountType = AccountType.LOCAL,
    thirdPartyAuthType: ThirdPartyAuthType?,
    thirdPartyAuthId: String?,
    resetPasswordCode: String?,
  ) : this(id = 0L, username = "", password, name = "") {
    this.permissions = permissions
    this.role = role
    this.accountType = accountType
    this.thirdPartyAuthType = thirdPartyAuthType
    this.thirdPartyAuthId = thirdPartyAuthId
    this.resetPasswordCode = resetPasswordCode
  }

  enum class Role {
    USER,
    ADMIN,
    SUPPORTER,
  }

  enum class AccountType {
    LOCAL,
    MANAGED,
    THIRD_PARTY,
  }

  @Transient
  override var disableActivityLogging: Boolean = false
}

fun UserAccount.isAdmin(): Boolean {
  return role == UserAccount.Role.ADMIN
}

fun UserAccount.isSupporter(): Boolean {
  return role == UserAccount.Role.SUPPORTER
}

fun UserAccount.isSupporterOrAdmin(): Boolean {
  return role == UserAccount.Role.SUPPORTER || role == UserAccount.Role.ADMIN
}
