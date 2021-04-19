package io.tolgee.model.import

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.issues.ImportArchiveIssue
import javax.persistence.*
import javax.validation.constraints.Size

@Entity
class ImportArchive(
        @Column(length = 2000)
        @field:Size(max = 2000)
        val name: String,

        @ManyToOne(cascade = [CascadeType.ALL], optional = false)
        val import: Import,
) : StandardAuditModel() {
    @OneToMany(mappedBy = "archive")
    var issues: MutableList<ImportArchiveIssue> = mutableListOf()

    @OneToMany(mappedBy = "archive")
    var files: MutableList<ImportFile> = mutableListOf()
}
