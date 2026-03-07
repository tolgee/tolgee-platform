package io.tolgee.model.qa

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id", unique = true),
  ],
)
class ProjectQaConfig(
  @OneToOne
  var project: Project,
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  var settings: MutableMap<QaCheckType, QaCheckSeverity> = mutableMapOf(),
  // TODO: somehow make sure the existing project have QA checks disabled while new projects use default settings for QA checks
  // this must be also extensible for new future QA checks - when we add new QA check it must be disable for all existing projects
  // but enabled for new projects (if it's default is enabled)
) : StandardAuditModel()
