package io.tolgee.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany

@Entity
class SsoConfig : StandardAuditModel() {
  @Column(name = "domain_name", unique = true, nullable = false)
  var domainName: String = ""

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "ssoConfig")
  var userAccounts: MutableSet<UserAccount> = mutableSetOf()
}
