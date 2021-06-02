package io.tolgee.model.key

import io.tolgee.dtos.PathDTO
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.Translation
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "name"], name = "key_project_id_name")])
data class Key(
        @field:NotBlank
        @field:Size(max = 2000)
        @Column(length = 2000)
        var name: String? = null,
) : StandardAuditModel() {
    @field:NotNull
    @ManyToOne(optional = false)
    var project: Project? = null

    @OneToMany(mappedBy = "key")
    var translations: MutableSet<Translation> = HashSet()

    @OneToOne(mappedBy = "key")
    var keyMeta: KeyMeta? = null

    constructor(name: String? = null,
                project: Project? = null,
                translations: MutableSet<Translation> = HashSet()
    ) : this(name) {
        this.project = project
        this.translations = translations
    }

    val path: PathDTO
        get() = PathDTO.fromFullPath(name)
}
