package io.tolgee.model.import

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.issues.ImportFileIssue
import javax.persistence.*
import javax.validation.constraints.Size

@Entity
class ImportFile(
        @field:Size(max = 2000)
        @Column(length = 2000)
        val name: String?,

        @ManyToOne(cascade = [CascadeType.ALL], optional = false)
        val import: Import,
) : StandardAuditModel() {
    @OneToMany(mappedBy = "file")
    var issues: MutableList<ImportFileIssue> = mutableListOf()

    @ManyToMany
    var keys: MutableList<ImportKey> = mutableListOf()

    @ManyToMany
    var languages: MutableList<ImportLanguage> = mutableListOf()

    @ManyToOne
    var archive: ImportFile? = null
}
