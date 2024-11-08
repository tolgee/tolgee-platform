package io.tolgee.model

import io.tolgee.component.ThirdPartyAuthTypeConverter
import io.tolgee.model.enums.ThirdPartyAuthType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.OneToOne
import java.util.*

@Entity
class AuthProviderChangeRequest : StandardAuditModel() {
  @Convert(converter = ThirdPartyAuthTypeConverter::class)
  var newAuthProvider: ThirdPartyAuthType? = null

  @Convert(converter = ThirdPartyAuthTypeConverter::class)
  var oldAuthProvider: ThirdPartyAuthType? = null

  var newAccountType: UserAccount.AccountType? = null

  var oldAccountType: UserAccount.AccountType? = null

  @OneToOne
  var userAccount: UserAccount? = null

  var newSsoDomain: String? = null

  var newSub: String? = null

  @Column(columnDefinition = "TEXT")
  var ssoRefreshToken: String? = null

  var ssoExpiration: Date? = null

  var isConfirmed: Boolean = false
}
