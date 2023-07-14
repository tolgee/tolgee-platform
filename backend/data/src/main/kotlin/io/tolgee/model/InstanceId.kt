package io.tolgee.model

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class InstanceId : AuditModel() {
  @field:Id
  val id: Int = 1

  var instanceId: String = UUID.randomUUID().toString()
}
