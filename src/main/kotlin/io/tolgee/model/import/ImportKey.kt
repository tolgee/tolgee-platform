package io.tolgee.model.import

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.import.issues.ImportFileIssue
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@Entity
class ImportKey(
        @field:NotBlank
        @field:Size(max = 2000)
        @Column(length = 2000)
        val name: String,

        @ManyToMany(mappedBy = "keys")
        @field:NotEmpty
        val files: List<ImportFile>,
) : StandardAuditModel()
