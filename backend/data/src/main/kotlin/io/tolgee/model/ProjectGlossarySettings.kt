package io.tolgee.model

import javax.persistence.Entity
import javax.validation.constraints.NotEmpty

@Entity
class ProjectGlossarySettings : StandardAuditModel() {
  @field:NotEmpty
  var name: String = ""
}
