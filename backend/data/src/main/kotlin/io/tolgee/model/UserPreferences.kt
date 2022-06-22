package io.tolgee.model

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

@Entity
class UserPreferences(
  @OneToOne
  @Id
  var userAccount: UserAccount? = null,

) : AuditModel() {
  var language: String? = null

  @ManyToOne
  var preferredOrganization: Organization? = null
}
