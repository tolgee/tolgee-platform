package io.tolgee.model

import com.vladmihalcea.hibernate.type.array.ListArrayType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MapsId
import javax.persistence.OneToOne

@Entity
@TypeDef(name = "string-array", typeClass = ListArrayType::class)
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

  @Type(type = "string-array")
  @Column(columnDefinition = "text[]")
  var completedSteps: MutableList<String> = mutableListOf()
}
