package io.tolgee.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity(name = "quick_start_guide")
@Table
class QuickStartGuide() : AuditModel() {
  @Id
  @Column()
  var id: Long = 0

  @Column()
  var completed = false

  @ManyToOne()
  var user: UserAccount? = null
}
