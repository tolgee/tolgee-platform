package io.tolgee.model.dataImport

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.Translation
import io.tolgee.model.dataImport.issues.ImportTranslationIssue
import io.tolgee.model.dataImport.issues.ImportTranslationIssueParam
import io.tolgee.model.dataImport.issues.issueTypes.TranslationIssueType
import io.tolgee.model.dataImport.issues.paramTypes.TranslationIssueParamType
import javax.persistence.*

@Entity
class ImportTranslation(
        @Column(columnDefinition = "text")
        val text: String?,

        @ManyToOne
        val language: ImportLanguage,

        ) : StandardAuditModel() {
    @OneToMany(mappedBy = "translation", cascade = [CascadeType.ALL])
    var issues: MutableList<ImportTranslationIssue> = mutableListOf()

    @ManyToOne(optional = false, cascade = [CascadeType.PERSIST])
    lateinit var key: ImportKey

    @OneToOne
    var collision: Translation? = null

    /**
     * Whether this translation will override the collision
     */
    var override: Boolean = false

    fun addIssue(
            type: TranslationIssueType,
            params: Map<TranslationIssueParamType, String>? = null) {
        val issue = ImportTranslationIssue(this, type)
        params?.map { ImportTranslationIssueParam(issue, it.key, it.value) }?.toMutableList()?.let {
            issue.params = it
        }
        this.issues.add(issue)
    }
}
