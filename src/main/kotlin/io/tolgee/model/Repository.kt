package io.tolgee.model

import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["address_part"], name = "repository_address_part_unique")])
data class Repository(
        @field:NotBlank
        @field:Size(min = 3, max = 500)
        var name: String? = null,

        private var description: String? = null,

        @Column(name = "address_part")
        @field:Size(min = 3, max = 500)
        @field:Pattern(regexp = "^[a-z0-9]*[a-z]+[a-z0-9]*$", message = "invalid_pattern")
        var addressPart: String? = null,

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0L,
) : AuditModel() {
    @OrderBy("abbreviation")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "repository")
    var languages: MutableSet<Language> = LinkedHashSet()


    @OneToMany(mappedBy = "repository")
    var permissions: MutableSet<Permission> = LinkedHashSet()

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "repository")
    var keys: MutableSet<Key> = LinkedHashSet()

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "repository")
    var apiKeys: MutableSet<ApiKey> = LinkedHashSet()

    fun getLanguage(abbreviation: String): Optional<Language> {
        return languages.stream().filter { l: Language -> (l.abbreviation == abbreviation) }.findFirst()
    }
}
