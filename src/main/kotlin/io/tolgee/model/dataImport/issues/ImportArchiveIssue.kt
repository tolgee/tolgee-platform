package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.ImportArchive
import io.tolgee.model.dataImport.issues.issueTypes.ArchiveIssueType
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull

@Entity
class ImportArchiveIssue(
        @ManyToOne(optional = false)
        @field:NotNull
        val archive: ImportArchive,

        @Enumerated
        val type: ArchiveIssueType
) : StandardAuditModel()
