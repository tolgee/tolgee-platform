package io.tolgee.model

import io.tolgee.dtos.request.LanguageDto
import io.tolgee.service.dataImport.ImportService
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional
import javax.persistence.*

@Entity
@EntityListeners(Language.Companion.LanguageListeners::class)
@Table(
        uniqueConstraints = [
            UniqueConstraint(
                    columnNames = ["project_id", "name"],
                    name = "language_project_name"
            ),
            UniqueConstraint(
                    columnNames = ["project_id", "tag"],
                    name = "language_tag_name")
        ],
        indexes = [
            Index(
                    columnList = "tag",
                    name = "index_tag"
            ),
            Index(
                    columnList = "tag, project_id",
                    name = "index_tag_project")
        ]
)
class Language : StandardAuditModel() {
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "language")
    var translations: MutableSet<Translation>? = null

    @ManyToOne
    var project: Project? = null

    @Column(nullable = false)
    var tag: String? = null

    var name: String? = null

    @Column(nullable = false)
    var originalName: String? = null

    fun updateByDTO(dto: LanguageDto) {
        name = dto.name
        tag = dto.tag
    }

    override fun toString(): String {
        return "Language(tag=$tag, name=$name, originalName=$originalName)"
    }

    companion object {
        @JvmStatic
        fun fromRequestDTO(dto: LanguageDto): Language {
            val language = Language()
            language.name = dto.name
            language.tag = dto.tag
            language.originalName = dto.originalName
            return language
        }

        @Configurable
        class LanguageListeners {
            @Autowired
            lateinit var provider: ObjectFactory<ImportService>

            @PreRemove
            @Transactional
            fun preRemove(language: Language) {
                provider.`object`.onExistingLanguageRemoved(language)
            }
        }
    }
}
