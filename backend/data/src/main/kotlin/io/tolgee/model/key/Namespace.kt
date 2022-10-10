package io.tolgee.model.key

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank

@Entity
@ActivityLoggedEntity
@ActivityReturnsExistence
class Namespace(
  @ActivityLoggedProp
  @ActivityDescribingProp
  @field:NotBlank
  var name: String = "",

  @ManyToOne
  var project: Project
) : StandardAuditModel()
