package io.tolgee.model

import io.tolgee.dtos.request.LanguageDTO
import javax.persistence.*

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(
        columnNames = ["repository_id", "name"],
        name = "language_repository_name"
    ), UniqueConstraint(columnNames = ["repository_id", "abbreviation"], name = "language_abbreviation_name")],
    indexes = [Index(
        columnList = "abbreviation",
        name = "index_abbreviation"
    ), Index(columnList = "abbreviation, repository_id", name = "index_abbreviation_repository")]
)
class Language : AuditModel() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "language")
    var translations: MutableSet<Translation>? = null

    @ManyToOne
    var repository: Repository? = null
    var abbreviation: String? = null
    var name: String? = null
    fun updateByDTO(dto: LanguageDTO) {
        name = dto.name
        abbreviation = dto.abbreviation
    }

    override fun toString(): String {
        return "Language(id=" + id + ", abbreviation=" + abbreviation + ", name=" + name + ")"
    }

    companion object {
        @JvmStatic
        fun fromRequestDTO(dto: LanguageDTO): Language {
            val language = Language()
            language.name = dto.name
            language.abbreviation = dto.abbreviation
            return language
        }
    }
}