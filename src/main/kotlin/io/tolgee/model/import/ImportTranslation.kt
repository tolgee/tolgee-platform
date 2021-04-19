package io.tolgee.model.import

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.issues.ImportTranslationIssue
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class ImportTranslation(
        @Column(columnDefinition = "text")
        val text: String,

        @ManyToOne(optional = false)
        val key: ImportKey,

        @ManyToOne
        val language: ImportLanguage,

        @OneToMany(mappedBy = "translation")
        val issues: List<ImportTranslationIssue>,
) : StandardAuditModel()
