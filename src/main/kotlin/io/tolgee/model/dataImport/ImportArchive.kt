package io.tolgee.model.dataImport

import io.tolgee.model.StandardAuditModel
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.validation.constraints.Size

@Entity
class ImportArchive(
        @Column(length = 2000)
        @field:Size(max = 2000)
        val name: String,

        @ManyToOne(optional = false)
        val import: Import,
) : StandardAuditModel() {

    @OneToMany(mappedBy = "archive")
    var files: MutableList<ImportFile> = mutableListOf()
}
