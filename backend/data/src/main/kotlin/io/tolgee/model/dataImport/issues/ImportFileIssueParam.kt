package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
  indexes = [
    Index(columnList = "issue_id"),
  ],
)
class ImportFileIssueParam(
  @ManyToOne(optional = false)
  val issue: ImportFileIssue,
  @Enumerated
  val type: FileIssueParamType,
  val value: String,
) : StandardAuditModel()
