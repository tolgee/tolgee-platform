package io.tolgee.model

import io.tolgee.model.enums.ApiScope
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

class Permission(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L,

  @ManyToOne(fetch = FetchType.LAZY)
  var user: UserAccount? = null,

  @OneToOne(fetch = FetchType.LAZY)
  var invitation: Invitation? = null,

  @Enumerated(EnumType.STRING)
  var type: ProjectPermissionType = ProjectPermissionType.VIEW
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
  ) : this(id, user, invitation, type) {
    this.project = project
  }

  @ManyToOne
  var project: Project = Project()

  enum class ProjectPermissionType(val power: Int, val availableScopes: Array<ApiScope>) {
    VIEW(1, arrayOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.SCREENSHOTS_VIEW, ApiScope.ACTIVITY_VIEW)),
    TRANSLATE(
      2,
      arrayOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.TRANSLATIONS_EDIT, ApiScope.SCREENSHOTS_VIEW, ApiScope.ACTIVITY_VIEW)
    ),
    EDIT(
      3,
      arrayOf(
        ApiScope.TRANSLATIONS_VIEW,
        ApiScope.TRANSLATIONS_EDIT,
        ApiScope.KEYS_EDIT,
        ApiScope.SCREENSHOTS_VIEW,
        ApiScope.SCREENSHOTS_UPLOAD,
        ApiScope.SCREENSHOTS_DELETE,
        ApiScope.ACTIVITY_VIEW
      )
    ),
    MANAGE(
      4,
      arrayOf(
        ApiScope.TRANSLATIONS_VIEW,
        ApiScope.TRANSLATIONS_EDIT,
        ApiScope.KEYS_EDIT,
        ApiScope.SCREENSHOTS_VIEW,
        ApiScope.SCREENSHOTS_UPLOAD,
        ApiScope.SCREENSHOTS_DELETE,
        ApiScope.ACTIVITY_VIEW
      )
    );
  }
}
