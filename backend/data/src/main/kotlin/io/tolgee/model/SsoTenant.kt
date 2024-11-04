package io.tolgee.model

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(name = "tenant")
class SsoTenant : StandardAuditModel() {
  var name: String = ""
  var ssoProvider: String = ""
  var clientId: String = ""
  var clientSecret: String = ""
  var authorizationUri: String = ""

  @Column(unique = true, nullable = false)
  var domain: String = ""
  var jwkSetUri: String = ""
  var tokenUri: String = ""

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  var organization: Organization? = null

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "ssoTenant")
  var userAccounts: MutableSet<UserAccount> = mutableSetOf()

  @ColumnDefault("true")
  var enabled: Boolean = true
}
