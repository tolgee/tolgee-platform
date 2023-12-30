package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne
import jakarta.validation.constraints.NotBlank

@Entity
class ImportFileIssueParam(
  @ManyToOne(optional = false)
  val issue: ImportFileIssue,
  @Enumerated
  val type: FileIssueParamType,
  @field:NotBlank
  val value: String,
) : StandardAuditModel()
