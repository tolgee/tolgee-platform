package io.tolgee.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
class InstanceId : AuditModel() {
  @field:Id
  val id: Int = 1

  var instanceId: String = UUID.randomUUID().toString()
}
