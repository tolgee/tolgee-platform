package io.tolgee.model.dataImport.issues

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.validation.constraints.NotNull

@Entity
class ImportFileIssue(
        @ManyToOne(optional = false)
        @field:NotNull
        var file: ImportFile,

        @Enumerated
        var type: FileIssueType = FileIssueType.NO_MATCHING_PROCESSOR,

        @OneToMany(mappedBy = "issue")
        var params: MutableList<ImportFileIssueParam>? = null,
) : StandardAuditModel()
