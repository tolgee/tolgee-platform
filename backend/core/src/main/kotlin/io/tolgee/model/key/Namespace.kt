package io.tolgee.model.key

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

@Entity
@ActivityLoggedEntity
@ActivityReturnsExistence
@Table(
  uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "name"], name = "namespace_name_project")],
  indexes = [Index(columnList = "name")],
)
class Namespace(
  @ActivityLoggedProp
  @ActivityDescribingProp
  @field:NotBlank
  @field:Length(max = 100)
  var name: String = "",
  @ManyToOne
  var project: Project,
) : StandardAuditModel()
