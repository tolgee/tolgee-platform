package io.tolgee.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MapsId
import javax.persistence.OneToOne

@Entity
class UserPreferences(
  @OneToOne
  @MapsId
  @JoinColumn(name = "user_account_id")
  var userAccount: UserAccount? = null,

) : AuditModel() {
  var language: String? = null

  @Id
  @Column(name = "user_account_id")
  var id: Long = 0

  @ManyToOne
  var preferredOrganization: Organization? = null
}
