package io.tolgee.model

import com.vladmihalcea.hibernate.type.array.ListArrayType
import io.tolgee.model.notifications.NotificationPreferences
import io.tolgee.model.notifications.UserNotification
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.Where
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.OrderBy
import javax.validation.constraints.NotBlank

@Entity
@TypeDef(name = "string-array", typeClass = ListArrayType::class)
data class UserAccount(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  override var id: Long = 0L,

  @field:NotBlank
  var username: String = "",

  var password: String? = null,

  var name: String = "",

  @Enumerated(EnumType.STRING)
  var role: Role? = Role.USER,

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type")
  var accountType: AccountType? = AccountType.LOCAL,
) : AuditModel(), ModelWithAvatar {
  @Column(name = "totp_key", columnDefinition = "bytea")
  var totpKey: ByteArray? = null

  @Type(type = "string-array")
  @Column(name = "mfa_recovery_codes", columnDefinition = "text[]")
  var mfaRecoveryCodes: List<String> = emptyList()

  @Column(name = "tokens_valid_not_before")
  var tokensValidNotBefore: Date? = null

  @OneToMany(mappedBy = "user", orphanRemoval = true)
  var permissions: MutableSet<Permission> = mutableSetOf()

  @OneToOne(mappedBy = "userAccount", fetch = FetchType.LAZY, optional = true)
  var emailVerification: EmailVerification? = null

  @Column(name = "third_party_auth_type")
  var thirdPartyAuthType: String? = null

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

  override var avatarHash: String? = null

  @Column(name = "deleted_at")
  var deletedAt: Date? = null

  @Column(name = "disabled_at")
  var disabledAt: Date? = null

  @Column(name = "is_initial_user", nullable = false)
  @ColumnDefault("false")
  var isInitialUser: Boolean = false

  @Column(name = "password_changed", nullable = false)
  @ColumnDefault("true")
  var passwordChanged: Boolean = true

  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], orphanRemoval = true, mappedBy = "recipient")
  var userNotifications: MutableList<UserNotification> = mutableListOf()

  @Where(clause = "project IS NOT NULL")
  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], orphanRemoval = true, mappedBy = "userAccount")
  var projectNotificationPreferences: MutableList<NotificationPreferences> = mutableListOf()

  @Where(clause = "project IS NULL")
  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], orphanRemoval = true, mappedBy = "userAccount")
  private var _globalNotificationPreferences: MutableList<NotificationPreferences> = mutableListOf()

  val globalNotificationPreferences: NotificationPreferences?
    get() = _globalNotificationPreferences.firstOrNull()

  val isDeletable: Boolean
    get() = this.accountType != AccountType.MANAGED && !this.isInitialUser

  val isMfaEnabled: Boolean
    get() = this.totpKey?.isNotEmpty() ?: false

  val needsSuperJwt: Boolean
    get() = this.accountType != AccountType.THIRD_PARTY || isMfaEnabled

  enum class Role {
    USER, ADMIN
  }

  enum class AccountType {
    LOCAL, MANAGED, THIRD_PARTY
  }
}
