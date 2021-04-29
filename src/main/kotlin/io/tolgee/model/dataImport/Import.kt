package io.tolgee.model.dataImport

import io.tolgee.model.Repository
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["author_id", "repository_id"])])
class Import(
        @field:NotNull
        @ManyToOne(optional = false)
        val author: UserAccount,

        @field:NotNull
        @ManyToOne(optional = false)
        val repository: Repository
) : StandardAuditModel() {
    @OneToMany(mappedBy = "import", cascade = [CascadeType.ALL])
    var archives = mutableListOf<ImportArchive>()

    @OneToMany(mappedBy = "import", cascade = [CascadeType.ALL])
    var files = mutableListOf<ImportFile>()
}
