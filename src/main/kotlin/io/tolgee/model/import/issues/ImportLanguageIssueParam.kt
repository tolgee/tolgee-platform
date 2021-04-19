package io.tolgee.model.import.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.ImportFile
import io.tolgee.model.import.issues.issueTypes.LanguageIssueType
import io.tolgee.model.import.issues.paramTypes.FileIssueParamType
import io.tolgee.model.import.issues.paramTypes.TranslationIssueParamType
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
class ImportLanguageIssueParam(
        @ManyToOne(optional = false)
        val issue: ImportLanguageIssue,

        @Enumerated
        val type: LanguageIssueType,

        @field:NotBlank
        val value: String
) : StandardAuditModel()
