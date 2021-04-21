package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.issues.issueTypes.LanguageIssueType
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
class ImportLanguageIssue(
        @ManyToOne(optional = false)
        @field:NotNull
        val language: ImportLanguage,

        @OneToMany(fetch = FetchType.EAGER, mappedBy = "issue")
        val params: List<ImportLanguageIssueParam>,

        @Enumerated
        val type: LanguageIssueType
) : StandardAuditModel()
