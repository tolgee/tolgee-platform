package io.tolgee.model.import.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.ImportFile
import io.tolgee.model.import.issues.issueTypes.FileIssueType
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
class ImportFileIssue(
        @ManyToOne(optional = false)
        @field:NotNull
        val file: ImportFile,

        @OneToMany(fetch = FetchType.EAGER, mappedBy = "issue")
        val params: List<ImportFileIssueParam>,

        @Enumerated
        val type: FileIssueType
) : StandardAuditModel()
