package io.tolgee.model

import io.hypersistence.utils.hibernate.type.array.StringArrayType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import org.hibernate.annotations.Type

@Entity
data class QuickStart(
  @OneToOne
  @MapsId
  @JoinColumn(name = "user_account_id")
  var userAccount: UserAccount,
) {

  @Id
  @Column(name = "user_account_id")
  var userAccountId: Long = 0

  @ManyToOne
  var organization: Organization? = null

  var finished = false

  var open: Boolean = true

  @Type(StringArrayType::class)
  @Column(columnDefinition = "text[]")
  var completedSteps: Array<String> = arrayOf()
}
