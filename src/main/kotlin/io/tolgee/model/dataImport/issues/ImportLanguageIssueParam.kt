package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.issues.issueTypes.LanguageIssueType
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank

@Entity
class ImportLanguageIssueParam(
        @ManyToOne(optional = false)
        val issue: ImportLanguageIssue,

        @Enumerated
        val type: LanguageIssueType,

        @field:NotBlank
        val value: String
) : StandardAuditModel()
