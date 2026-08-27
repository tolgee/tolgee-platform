package io.tolgee.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.Date

@Entity
class ForcedServerDateTime {
  @Id
  val id = 1

  var time: Date = Date()
}
