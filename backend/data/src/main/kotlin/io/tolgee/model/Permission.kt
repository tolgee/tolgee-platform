package io.tolgee.model

import com.vladmihalcea.hibernate.type.array.EnumArrayType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.unpack
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

@Suppress("LeakingThis")
@Entity
@TypeDef(
  name = "enum-array",
  typeClass = EnumArrayType::class,
  parameters = [
    Parameter(
      name = EnumArrayType.SQL_ARRAY_TYPE,
      value = "varchar"
    )
  ]
)
class Permission(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L,

  @ManyToOne(fetch = FetchType.LAZY)
  var user: UserAccount? = null,

  /**
   * When base permission for organization
   */
  @OneToOne(fetch = FetchType.LAZY)
  var organization: Organization? = null,

  @OneToOne(fetch = FetchType.LAZY)
  var invitation: Invitation? = null,

  @Type(type = "enum-array")
  @Column(name = "scopes", columnDefinition = "varchar[]")
  var scopes: Array<Scope> = ProjectPermissionType.VIEW.availableScopes
) : AuditModel() {

  var type: ProjectPermissionType?
    set(value) {
      value?.let {
        scopes = it.availableScopes
      }
    }
    get() {
      return ProjectPermissionType.values().find {
        val unpackedAvailableScopes = it.availableScopes.unpack()
        val unpackedCurrentScopes = scopes.unpack()
        val containsAll = unpackedAvailableScopes.toList().containsAll(
          unpackedCurrentScopes.toList()
        )
        val hasSameSize = unpackedAvailableScopes.size == unpackedCurrentScopes.size
        hasSameSize && containsAll
      }
    }

  /**
   * Kept only to keep data in the DB, before migrated
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  private var deprecatedType: ProjectPermissionType = ProjectPermissionType.VIEW

  /**
   * Languages for translate permission.
   * When specified, user is restricted to edit/review specific language translations.
   *
   * This field makes no sense for any other permission type.
   */
  @ManyToMany(fetch = FetchType.EAGER)
  var languages: MutableSet<Language> = mutableSetOf()

  constructor(
    id: Long = 0L,
    user: UserAccount? = null,
    invitation: Invitation? = null,
    project: Project? = null,
    organization: Organization? = null,
    type: ProjectPermissionType = ProjectPermissionType.VIEW
  ) : this(id, user, null, invitation, scopes = type.availableScopes) {
    this.project = project
  }

  @ManyToOne
  var project: Project? = null
}
