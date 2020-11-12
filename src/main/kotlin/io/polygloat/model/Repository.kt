package io.polygloat.model

import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["name", "created_by_id"], name = "repository_name_created_by_id")])
data class Repository(
        @ManyToOne
        var createdBy: UserAccount? = null,

        @NotBlank @Size(min = 3, max = 500)
        var name: String? = null,

        private var description: String? = null,

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0L,

        @OrderBy("abbreviation")
        @OneToMany(fetch = FetchType.LAZY, mappedBy = "repository")
        var languages: MutableSet<Language> = LinkedHashSet(),

        @OneToMany(mappedBy = "repository")
        var permissions: MutableSet<Permission> = LinkedHashSet(),

        @OneToMany(fetch = FetchType.LAZY, mappedBy = "repository")
        var sources: MutableSet<Source> = LinkedHashSet(),

        @OneToMany(fetch = FetchType.LAZY, mappedBy = "repository")
        var apiKeys: MutableSet<ApiKey> = LinkedHashSet(),
) : AuditModel() {
    fun getLanguage(abbreviation: String): Optional<Language> {
        return languages.stream().filter { l: Language -> (l.abbreviation == abbreviation) }.findFirst()
    }
}