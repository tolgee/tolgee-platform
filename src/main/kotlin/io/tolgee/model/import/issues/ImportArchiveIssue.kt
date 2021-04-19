package io.tolgee.model.import.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.ImportArchive
import io.tolgee.model.import.ImportFile
import io.tolgee.model.import.issues.issueTypes.ArchiveIssueType
import io.tolgee.model.import.issues.issueTypes.FileIssueType
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
class ImportArchiveIssue(
        @ManyToOne(optional = false)
        @field:NotNull
        val archive: ImportArchive,

        @Enumerated
        val type: ArchiveIssueType
) : StandardAuditModel()
