package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank

@Entity
class ImportFileIssueParam(
  @ManyToOne(optional = false)
  val issue: ImportFileIssue,

  @Enumerated
  val type: FileIssueParamType,

  @field:NotBlank
  val value: String
) : StandardAuditModel()
