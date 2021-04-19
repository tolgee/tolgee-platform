package io.tolgee.model.import

import io.tolgee.model.StandardAuditModel
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@Entity
class ImportLanguage(
        val name: String,

        @ManyToMany(mappedBy = "languages")
        @field:NotEmpty
        val files: List<ImportFile>,

        @OneToMany(mappedBy = "language")
        val translations: List<ImportTranslation>
) : StandardAuditModel()
