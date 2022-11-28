package io.tolgee.model

import com.vladmihalcea.hibernate.type.array.EnumArrayType
import io.tolgee.model.enums.Scope
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
   * When base permission for organizatio
   */
  @OneToOne(fetch = FetchType.LAZY)
  var organization: Organization? = null,

  @OneToOne(fetch = FetchType.LAZY)
  var invitation: Invitation? = null,

  @Enumerated(EnumType.STRING)
  var type: ProjectPermissionType = ProjectPermissionType.VIEW,

  @Type(type = "enum-array")
  @Column(name = "scopes", columnDefinition = "varchar[]")
  var scopes: Array<Scope> = ProjectPermissionType.VIEW.availableScopes
) : AuditModel() {

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
    project: Project,
    type: ProjectPermissionType = ProjectPermissionType.VIEW
  ) : this(id, user, null, invitation, type) {
    this.project = project
  }

  @ManyToOne
  var project: Project? = null

  enum class ProjectPermissionType(val power: Int, val availableScopes: Array<Scope>) {
    VIEW(1, arrayOf(Scope.TRANSLATIONS_VIEW, Scope.SCREENSHOTS_VIEW, Scope.ACTIVITY_VIEW)),
    TRANSLATE(
      2,
      arrayOf(Scope.TRANSLATIONS_VIEW, Scope.TRANSLATIONS_EDIT, Scope.SCREENSHOTS_VIEW, Scope.ACTIVITY_VIEW)
    ),
    EDIT(
      3,
      arrayOf(
        Scope.TRANSLATIONS_VIEW,
        Scope.TRANSLATIONS_EDIT,
        Scope.KEYS_EDIT,
        Scope.SCREENSHOTS_VIEW,
        Scope.SCREENSHOTS_UPLOAD,
        Scope.SCREENSHOTS_DELETE,
        Scope.ACTIVITY_VIEW,
        Scope.IMPORT
      )
    ),
    MANAGE(
      4,
      arrayOf(
        Scope.TRANSLATIONS_VIEW,
        Scope.TRANSLATIONS_EDIT,
        Scope.KEYS_EDIT,
        Scope.SCREENSHOTS_VIEW,
        Scope.SCREENSHOTS_UPLOAD,
        Scope.SCREENSHOTS_DELETE,
        Scope.ACTIVITY_VIEW,
        Scope.IMPORT,
        Scope.LANGUAGES_EDIT
      )
    );
  }
}
