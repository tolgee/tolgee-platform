package io.tolgee.model

import com.vladmihalcea.hibernate.type.array.ListArrayType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.*

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
  var id: Long = 0

  @Column(name = "guide_open")
  var open = true

  @Type(type = "string-array")
  @Column(name = "guide_completed_steps", columnDefinition = "text[]")
  var completedSteps: MutableList<String> = mutableListOf()
}
