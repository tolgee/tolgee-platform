package io.polygloat.model

import com.unboundid.util.Mutable
import io.polygloat.dtos.PathDTO
import java.util.*
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["repository_id", "name"], name = "source_repository_id_name")])
//Todo: Rename to Key
data class Source(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        var name: String? = null,
) : AuditModel() {
    @ManyToOne
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
        return translations.stream().filter { t: Translation -> t.language.abbreviation == abbr }.findFirst()
    }

    val path: PathDTO
        get() = PathDTO.fromFullPath(name)
}