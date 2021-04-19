package io.tolgee.model.import.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.ImportFile
import io.tolgee.model.import.ImportLanguage
import io.tolgee.model.import.ImportTranslation
import io.tolgee.model.import.issues.issueTypes.FileIssueType
import io.tolgee.model.import.issues.issueTypes.LanguageIssueType
import io.tolgee.model.import.issues.issueTypes.TranslationIssueType
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
class ImportTranslationIssue(
        @ManyToOne(optional = false)
        @field:NotNull
        val translation: ImportTranslation,

        @OneToMany(fetch = FetchType.EAGER, mappedBy = "issue")
        val params: List<ImportTranslationIssueParam>,

        @Enumerated
        val type: TranslationIssueType
) : StandardAuditModel()
