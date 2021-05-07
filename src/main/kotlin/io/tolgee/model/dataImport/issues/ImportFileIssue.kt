package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
class ImportFileIssue(
        @ManyToOne(optional = false)
        @field:NotNull
        var file: ImportFile,

        @Enumerated
        var type: FileIssueType = FileIssueType.NO_MATCHING_PROCESSOR,

        @OneToMany(fetch = FetchType.EAGER, mappedBy = "issue", cascade = [CascadeType.ALL])
        var params: MutableList<ImportFileIssueParam>? = null,
) : StandardAuditModel()
