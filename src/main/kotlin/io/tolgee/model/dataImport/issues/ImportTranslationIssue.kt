package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.issueTypes.TranslationIssueType
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
class ImportTranslationIssue(
        @ManyToOne(optional = false)
        @field:NotNull
        val translation: ImportTranslation,

        @Enumerated
        val type: TranslationIssueType,
) : StandardAuditModel() {
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "issue")
    var params: MutableList<ImportTranslationIssueParam> = mutableListOf()
}
