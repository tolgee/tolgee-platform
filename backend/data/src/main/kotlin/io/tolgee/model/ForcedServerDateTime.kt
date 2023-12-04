package io.tolgee.model

import org.springframework.data.annotation.AccessType
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Temporal

@Entity
class ForcedServerDateTime {
  @Id
  @AccessType(AccessType.Type.PROPERTY)
  val id = 1

  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  var time: Date = Date()
}
