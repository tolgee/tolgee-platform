package io.tolgee.model.dataImport

import io.tolgee.model.Language
import io.tolgee.model.StandardAuditModel
import javax.persistence.*
import javax.validation.constraints.Size

@Entity
class ImportLanguage(
        @Size(max = 2000)
        @Column(length = 2000)
        var name: String,

        @ManyToOne(optional = false)
        var file: ImportFile
) : StandardAuditModel() {
    @OneToMany(mappedBy = "language", cascade = [CascadeType.ALL])
    var translations: MutableList<ImportTranslation> = mutableListOf()

    @ManyToOne
    var existingLanguage: Language? = null
}
