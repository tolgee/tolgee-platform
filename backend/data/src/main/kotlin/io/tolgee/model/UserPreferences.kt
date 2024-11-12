package io.tolgee.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(
  indexes = [
    Index(columnList = "preferred_organization_id"),
  ],
)
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

  @ManyToOne(fetch = FetchType.LAZY)
  var preferredOrganization: Organization? = null

  @Id
  @Column(name = "user_account_id")
  var id: Long = 0
}
