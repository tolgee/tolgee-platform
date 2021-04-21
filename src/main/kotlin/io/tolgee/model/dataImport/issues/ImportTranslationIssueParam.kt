package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.issues.paramTypes.TranslationIssueParamType
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank

@Entity
class ImportTranslationIssueParam(
        @ManyToOne(optional = false)
        val issue: ImportTranslationIssue,

        @Enumerated
        val type: TranslationIssueParamType,

        @field:NotBlank
        val value: String
) : StandardAuditModel()
