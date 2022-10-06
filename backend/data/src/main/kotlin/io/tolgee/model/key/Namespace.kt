package io.tolgee.model.key

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@ActivityLoggedEntity
@ActivityReturnsExistence
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "name"], name = "namespace_name_project")])
class Namespace(
  @ActivityLoggedProp
  @ActivityDescribingProp
  var name: String? = null,

  @ManyToOne
  var project: Project
) : StandardAuditModel()
