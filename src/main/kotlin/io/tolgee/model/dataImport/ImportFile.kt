package io.tolgee.model.dataImport

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.issues.ImportFileIssue
import io.tolgee.model.dataImport.issues.ImportFileIssueParam
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import javax.persistence.*
import javax.validation.constraints.Size

@Entity
class ImportFile(
        @field:Size(max = 2000)
        @Column(length = 2000)
        val name: String?,

        @ManyToOne(optional = false)
        val import: Import,
) : StandardAuditModel() {
    @OneToMany(mappedBy = "file", cascade = [CascadeType.ALL])
    var issues: MutableList<ImportFileIssue> = mutableListOf()

    @ManyToMany(cascade = [CascadeType.PERSIST])
    var keys: MutableList<ImportKey> = mutableListOf()

    @OneToMany(mappedBy = "file")
    var languages: MutableList<ImportLanguage> = mutableListOf()

    @ManyToOne
    var archive: ImportFile? = null

    fun addIssue(type: FileIssueType, params: Map<FileIssueParamType, String>) {
        this.issues.add(ImportFileIssue(file = this, type = type).apply {
            this.params = params.map { ImportFileIssueParam(this, it.key, it.value) }.toMutableList()
        })
    }
}
