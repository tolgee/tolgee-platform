package io.tolgee.model.key

import io.tolgee.dtos.PathDTO
import io.tolgee.model.Repository
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.Translation
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["repository_id", "name"], name = "key_repository_id_name")])
data class Key(
        @field:NotBlank
        @field:Size(max = 2000)
        @Column(length = 2000)
        var name: String? = null,
) : StandardAuditModel() {
    @field:NotNull
    @ManyToOne(optional = false)
    var repository: Repository? = null

    @OneToMany(mappedBy = "key")
    var translations: MutableSet<Translation> = HashSet()

    @OneToOne(mappedBy = "key")
    var keyMeta: KeyMeta? = null

    constructor(name: String? = null,
                repository: Repository? = null,
                translations: MutableSet<Translation> = HashSet()
    ) : this(name) {
        this.repository = repository
        this.translations = translations
    }

    val path: PathDTO
        get() = PathDTO.fromFullPath(name)
}
