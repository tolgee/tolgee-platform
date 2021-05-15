package io.tolgee.model.key

import io.tolgee.dtos.PathDTO
import io.tolgee.model.AuditModel
import io.tolgee.model.Repository
import io.tolgee.model.Translation
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["repository_id", "name"], name = "key_repository_id_name")])
data class Key(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @field:NotBlank
        @field:Size(max = 2000)
        @Column(length = 2000)
        var name: String? = null,
) : AuditModel() {
    @field:NotNull
    @ManyToOne(optional = false)
    var repository: Repository? = null

    @OneToMany(mappedBy = "key")
    var translations: MutableSet<Translation> = HashSet()

    constructor(id: Long? = null,
                name: String? = null,
                repository: Repository? = null,
                translations: MutableSet<Translation> = HashSet()
    ) : this(id, name) {
        this.repository = repository
        this.translations = translations
    }

    fun getTranslation(abbr: String): Optional<Translation> {
        return translations.stream().filter { t: Translation -> t.language!!.abbreviation == abbr }.findFirst()
    }

    val path: PathDTO
        get() = PathDTO.fromFullPath(name)
}
