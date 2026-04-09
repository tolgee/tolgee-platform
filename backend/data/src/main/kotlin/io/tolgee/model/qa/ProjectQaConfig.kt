package io.tolgee.model.qa

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
  ],
)
class ProjectQaConfig(
  @OneToOne
  @JoinColumn(nullable = false)
  var project: Project,
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  var settings: MutableMap<QaCheckType, QaCheckSeverity> = mutableMapOf(),
) : StandardAuditModel()
