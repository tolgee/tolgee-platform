package io.tolgee.model

import io.tolgee.activity.ActivityLogged
import io.tolgee.model.key.Key
import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.OrderBy
import javax.persistence.PrePersist
import javax.persistence.PreUpdate
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity
@EntityListeners(Project.Companion.ProjectListener::class)
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["address_part"], name = "project_address_part_unique")])
@Audited
@ActivityLogged
class Project(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  override var id: Long = 0L,

  @field:NotBlank
  @field:Size(min = 3, max = 50)
  @ActivityLogged
  var name: String = "",

  @field:Size(min = 3, max = 2000)
  @ActivityLogged
  var description: String? = null,

  @Column(name = "address_part")
  @ActivityLogged
  @field:Size(min = 3, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-]*$", message = "invalid_pattern")
  var slug: String? = null,
) : AuditModel(), ModelWithAvatar {

  @OrderBy("id")
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
  var languages: MutableSet<Language> = LinkedHashSet()

  @OneToMany(mappedBy = "project")
  var permissions: MutableSet<Permission> = LinkedHashSet()

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
  var keys: MutableSet<Key> = LinkedHashSet()

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
  var apiKeys: MutableSet<ApiKey> = LinkedHashSet()

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  var userOwner: UserAccount? = null

  @ManyToOne(optional = true)
  var organizationOwner: Organization? = null

  @OneToOne(fetch = FetchType.LAZY)
  @ActivityLogged
  var baseLanguage: Language? = null

  @ActivityLogged
  @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], mappedBy = "project")
  var autoTranslationConfig: AutoTranslationConfig? = null

  @ActivityLogged
  override var avatarHash: String? = null

  constructor(name: String, description: String? = null, slug: String?, userOwner: UserAccount?) :
    this(id = 0L, name, description, slug) {
      this.userOwner = userOwner
    }

  constructor(
    name: String,
    description: String? = null,
    slug: String?,
    organizationOwner: Organization?,
    userOwner: UserAccount? = null
  ) :
    this(id = 0L, name, description, slug) {
      this.organizationOwner = organizationOwner
      this.userOwner = userOwner
    }

  fun getLanguage(tag: String): Optional<Language> {
    return languages.stream().filter { l: Language -> (l.tag == tag) }.findFirst()
  }

  companion object {
    class ProjectListener {
      @PrePersist
      @PreUpdate
      fun preSave(project: Project) {
        if (!(project.organizationOwner == null).xor(project.userOwner == null)) {
          throw Exception("Exactly one of organizationOwner or userOwner must be set!")
        }
      }
    }
  }
}
