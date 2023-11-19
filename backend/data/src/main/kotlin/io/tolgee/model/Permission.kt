package io.tolgee.model

import com.vladmihalcea.hibernate.type.array.EnumArrayType
import io.tolgee.dtos.cacheable.IPermission
import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
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
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
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
  private var _scopes: Array<Scope>? = null
    set(value) {
      field = value
      if (!value.isNullOrEmpty()) {
        this.type = null
      }
    }

  override var scopes: Array<Scope>
    get() = _scopes ?: type?.availableScopes ?: throw IllegalStateException()
    set(value) {
      this._scopes = value
    }

  override val granular: Boolean
    get() = this._scopes != null

  /**
   * When user doesn't have granular permission set
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  override var type: ProjectPermissionType? = ProjectPermissionType.VIEW

  /**
   * Languages for TRANSLATIONS_EDIT scope.
   * When specified, user is restricted to edit specific language translations.
   */
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "permission_languages", inverseJoinColumns = [JoinColumn(name = "languages_id")])
  var translateLanguages: MutableSet<Language> = mutableSetOf()

  /**
   * Languages for TRANSLATIONS_EDIT scope.
   * When specified, user is restricted to edit specific language translations.
   */
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "permission_view_languages")
  var viewLanguages: MutableSet<Language> = mutableSetOf()

  /**
   * Languages for TRANSLATIONS_EDIT scope.
   * When specified, user is restricted to edit specific language translations.
   */
  @ManyToMany(fetch = FetchType.EAGER)
  var stateChangeLanguages: MutableSet<Language> = mutableSetOf()

  constructor(
    id: Long = 0L,
    user: UserAccount? = null,
    invitation: Invitation? = null,
    project: Project? = null,
    organization: Organization? = null,
    type: ProjectPermissionType? = ProjectPermissionType.VIEW,
    languagePermissions: LanguagePermissions? = null,
    scopes: Array<Scope>? = null
  ) : this(
    id = id,
    user = user,
    organization = null,
    invitation = invitation
  ) {
    this._scopes = scopes
    this.project = project
    this.type = type
    this.organization = organization
    this.viewLanguages = languagePermissions?.view?.toMutableSet() ?: mutableSetOf()
    this.translateLanguages = languagePermissions?.translate?.toMutableSet() ?: mutableSetOf()
    this.stateChangeLanguages = languagePermissions?.stateChange?.toMutableSet() ?: mutableSetOf()
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
  override val translateLanguageIds: Set<Long>?
    get() = this.translateLanguages.map { it.id }.toSet()

  override val viewLanguageIds: Set<Long>?
    get() = this.viewLanguages.map { it.id }.toSet()

  override val stateChangeLanguageIds: Set<Long>?
    get() = this.stateChangeLanguages.map { it.id }.toSet()

  companion object {

    class PermissionListeners {
      @PrePersist
      @PreUpdate
      fun prePersist(permission: Permission) {
        if (permission._scopes?.isEmpty() == true) {
          permission._scopes = null
        }
        if (!((permission._scopes == null) xor (permission.type == null))) {
          throw IllegalStateException("Exactly one of scopes or type has to be set")
        }
        if (permission.organization != null && (
          permission.viewLanguages.isNotEmpty() ||
            permission.translateLanguages.isNotEmpty() ||
            permission.stateChangeLanguages.isNotEmpty()
          )
        ) {
          throw IllegalStateException("Organization base permission cannot have language permissions")
        }
      }
    }
  }
}
