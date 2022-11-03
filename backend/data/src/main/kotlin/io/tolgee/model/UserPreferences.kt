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
  var userAccount: UserAccount,
) : AuditModel() {

  constructor(userAccount: UserAccount, preferredOrganization: Organization?) : this(userAccount) {
    this.preferredOrganization = preferredOrganization
  }

  var language: String? = null

  @ManyToOne
  var preferredOrganization: Organization? = null

  @Id
  @Column(name = "user_account_id")
  var id: Long = 0
}
