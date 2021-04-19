package io.tolgee.model.import

import io.tolgee.model.Language
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.issues.ImportFileIssue
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@Entity
class ImportFile(
        @field:NotBlank
        @field:Size(max = 2000)
        val name: String,

        @ManyToOne(cascade = [CascadeType.ALL], optional = false)
        val import: Import,

        @OneToMany(mappedBy = "file")
        val issues: List<ImportFileIssue>,

        @ManyToMany
        @field:NotEmpty
        val keys: List<ImportKey>,

        @ManyToMany
        @field:NotEmpty
        val languages: List<ImportLanguage>,

        @ManyToOne
        val archive: ImportArchive
) : StandardAuditModel()
