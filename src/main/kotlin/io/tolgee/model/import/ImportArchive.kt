package io.tolgee.model.import

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.issues.ImportArchiveIssue
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class ImportArchive(
        @ManyToOne(cascade = [CascadeType.ALL], optional = false)
        val import: Import,

        @OneToMany(mappedBy = "archive")
        val issues: List<ImportArchiveIssue>,

        @OneToMany(mappedBy = "archive")
        val files: List<ImportFile>
) : StandardAuditModel()
