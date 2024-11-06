package io.tolgee.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(name = "tenant")
class SsoTenant : StandardAuditModel() {
  var name: String = ""
  var clientId: String = ""
  var clientSecret: String = ""
  var authorizationUri: String = ""

  @Column(unique = true, nullable = false)
  @NotBlank
  var domain: String = ""
  var jwkSetUri: String = ""
  var tokenUri: String = ""

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  lateinit var organization: Organization

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "ssoTenant")
  var userAccounts: MutableSet<UserAccount> = mutableSetOf()

  @ColumnDefault("true")
  var enabled: Boolean = true
}
