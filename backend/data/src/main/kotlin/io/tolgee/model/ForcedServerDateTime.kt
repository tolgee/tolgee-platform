package io.tolgee.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Temporal
import java.util.*

@Entity
class ForcedServerDateTime {
  @Id
  val id = 1

  @Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
  var time: Date = Date()
}
