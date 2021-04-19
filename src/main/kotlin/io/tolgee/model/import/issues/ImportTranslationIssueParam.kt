package io.tolgee.model.import.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.ImportFile
import io.tolgee.model.import.issues.paramTypes.FileIssueParamType
import io.tolgee.model.import.issues.paramTypes.TranslationIssueParamType
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
class ImportTranslationIssueParam(
        @ManyToOne(optional = false)
        val issue: ImportTranslationIssue,

        @Enumerated
        val type: TranslationIssueParamType,

        @field:NotBlank
        val value: String
) : StandardAuditModel()
