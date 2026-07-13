package io.tolgee.model

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.tolgee.dtos.cacheable.IPermission
import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.translationAgency.TranslationAgency
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.springframework.beans.factory.annotation.Configurable

@Suppress("LeakingThis")
@Entity
@EntityListeners(Permission.Companion.PermissionListeners::class)
@Table(
  indexes = [
    Index(columnList = "user_id"),
    Index(columnList = "project_id"),
    Index(columnList = "agency_id"),
  ],
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
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  var agency: TranslationAgency? = null,
) : AuditModel(),
  IPermission {
  @Type(
    EnumArrayType::class,
    parameters = [
      Parameter(
        name = EnumArrayType.SQL_ARRAY_TYPE,
        value = "varchar",
      ),
    ],
  )
  @Column(name = "scopes", columnDefinition = "varchar[]")
  private var _scopes: Array<Scope>? = null
    set(value) {
      field = value
      if (!value.isNullOrEmpty()) {
        this.type = null
      }
    }

  override var scopes: Array<Scope>
    get() = getScopesFromTypeAndScopes(this.type, this._scopes)
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
   * Languages for TRANSLATIONS_SUGGEST scope.
   * When specified, user is restricted to suggest specific language translations.
   */
  @ManyToMany(fetch = FetchType.EAGER)
  var suggestLanguages: MutableSet<Language> = mutableSetOf()

  /**
   * Languages for TRANSLATIONS_VIEW scope.
   * When specified, user is restricted to view specific language translations.
   */
  @ManyToMany(fetch = FetchType.EAGER)
  var viewLanguages: MutableSet<Language> = mutableSetOf()

  /**
   * Languages for TRANSLATIONS_STATE_EDIT scope.
   * When specified, user is restricted to change state only to specific language translations.
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
    scopes: Array<Scope>? = null,
    agency: TranslationAgency? = null,
  ) : this(
    id = id,
    user = user,
    organization = null,
    invitation = invitation,
  ) {
    this._scopes = scopes
    this.project = project
    this.type = type
    this.organization = organization
    this.viewLanguages = languagePermissions?.view?.toMutableSet() ?: mutableSetOf()
    this.translateLanguages = languagePermissions?.translate?.toMutableSet() ?: mutableSetOf()
    this.stateChangeLanguages = languagePermissions?.stateChange?.toMutableSet() ?: mutableSetOf()
    this.suggestLanguages = languagePermissions?.suggest?.toMutableSet() ?: mutableSetOf()
    this.agency = agency
  }

  @ManyToOne
  var project: Project? = null

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

  override val suggestLanguageIds: Set<Long>?
    get() = this.suggestLanguages.map { it.id }.toSet()

  companion object {
    @Configurable
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
        if (permission.organization != null &&
          (
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
