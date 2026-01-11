package io.tolgee.model

import io.tolgee.component.ThirdPartyAuthTypeConverter
import io.tolgee.model.enums.ThirdPartyAuthType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.OneToOne
import java.util.Date

@Entity
class AuthProviderChangeRequest : StandardAuditModel() {
  @OneToOne(optional = false)
  var userAccount: UserAccount? = null

  var expirationDate: Date? = null

  var identifier: String? = null

  @Convert(converter = ThirdPartyAuthTypeConverter::class)
  var authType: ThirdPartyAuthType? = null

  var authId: String? = null

  var ssoDomain: String? = null

  @Column(columnDefinition = "TEXT")
  var ssoRefreshToken: String? = null

  var ssoExpiration: Date? = null
}
