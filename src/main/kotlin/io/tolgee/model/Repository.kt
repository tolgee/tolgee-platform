package io.tolgee.model

import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity
@EntityListeners(Repository.Companion.RepositoryListener::class)
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

    @ManyToOne(optional = true)
    var userOwner: UserAccount? = null

    @ManyToOne(optional = true)
    var organizationOwner: UserAccount? = null

    fun getLanguage(abbreviation: String): Optional<Language> {
        return languages.stream().filter { l: Language -> (l.abbreviation == abbreviation) }.findFirst()
    }

    companion object {
        class RepositoryListener {
            @PrePersist
            fun prePersist(repository: Repository) {
                if (repository.organizationOwner == null && repository.userOwner == null) {
                    throw Exception("User owner or organization owner must be set")
                }

                if (repository.organizationOwner != null && repository.userOwner != null) {
                    throw Exception("Cannot set both organization owner and user owner")
                }
            }

            @PreUpdate
            fun preUpdate(repository: Repository) {

            }
        }
    }

}
