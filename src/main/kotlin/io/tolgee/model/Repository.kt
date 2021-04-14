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
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0L,

        @field:NotBlank
        @field:Size(min = 3, max = 500)
        var name: String? = null,

        @field:Size(min = 3, max = 2000)
        var description: String? = null,

        @Column(name = "address_part")
        @field:Size(min = 3, max = 50)
        @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-]*$", message = "invalid_pattern")
        var addressPart: String? = null,
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
    var organizationOwner: Organization? = null

    constructor(name: String?, description: String? = null, addressPart: String?, userOwner: UserAccount?)
            : this(id = 0L, name, description, addressPart) {
        this.userOwner = userOwner
    }

    constructor(name: String?,
                description: String? = null,
                addressPart: String?,
                organizationOwner: Organization?,
                userOwner: UserAccount? = null)
            : this(id = 0L, name, description, addressPart) {
        this.organizationOwner = organizationOwner
        this.userOwner = userOwner
    }

    fun getLanguage(abbreviation: String): Optional<Language> {
        return languages.stream().filter { l: Language -> (l.abbreviation == abbreviation) }.findFirst()
    }

    companion object {
        class RepositoryListener {
            @PrePersist
            @PreUpdate
            fun preSave(repository: Repository) {
                if (!(repository.organizationOwner == null).xor(repository.userOwner == null)) {
                    throw Exception("Exactly one of organizationOwner or userOwner must be set!")
                }
            }
        }
    }

}
