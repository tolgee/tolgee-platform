package io.tolgee.model

import io.hypersistence.utils.hibernate.type.array.StringArrayType
import io.tolgee.dtos.queryResults.organization.IQuickStart
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "organization_id"),
  ],
)
data class QuickStart(
  @OneToOne
  @MapsId
  @JoinColumn(name = "user_account_id")
  var userAccount: UserAccount,
) : IQuickStart {
  @Id
  @Column(name = "user_account_id")
  var userAccountId: Long = 0

  @ManyToOne
  var organization: Organization? = null

  override var finished = false

  override var open: Boolean = true

  @Type(StringArrayType::class)
  @Column(columnDefinition = "text[]")
  override var completedSteps: Array<String> = arrayOf()
}
