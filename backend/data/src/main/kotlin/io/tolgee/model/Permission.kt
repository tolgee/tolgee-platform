package io.tolgee.model

import com.vladmihalcea.hibernate.type.array.EnumArrayType
import io.tolgee.dtos.cacheable.IPermission
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.unpack
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.PrePersist
import javax.persistence.PreUpdate

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
@EntityListeners(Permission.Companion.PermissionListeners::class)
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
) : AuditModel(), IPermission {
  @Type(type = "enum-array")
  @Column(name = "scopes", columnDefinition = "varchar[]")
  private var _scopes: Array<Scope>? = ProjectPermissionType.VIEW.availableScopes

  override var scopes: Array<Scope>
    get() = _scopes ?: type?.availableScopes ?: throw IllegalStateException()
    set(value) {
      this._scopes = value
    }

  override val granular: Boolean
    get() = this._scopes != null

  var estimatedTypeFromScopes: ProjectPermissionType?
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
   * When user doesn't have granular permission set
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  override var type: ProjectPermissionType? = ProjectPermissionType.VIEW

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
  ) : this(id, user, null, invitation) {
    this.project = project
    this.scopes = type.availableScopes
  }

  @ManyToOne
  var project: Project? = null

  val userId: Long?
    get() = this.user?.id
  val invitationId: Long?
    get() = this.invitation?.id
  override val projectId: Long?
    get() = this.project?.id
  override val organizationId: Long?
    get() = this.organization?.id
  override val languageIds: Set<Long>?
    get() = this.languages.map { it.id }.toSet()

  companion object {

    class PermissionListeners {
      @PrePersist
      @PreUpdate
      fun prePersist(permission: Permission) {
        if (permission._scopes.isNullOrEmpty() && permission.type == null) {
          throw IllegalStateException("Cannot save permission with no scopes or type")
        }
      }
    }
  }
}
