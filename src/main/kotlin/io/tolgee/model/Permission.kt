package io.tolgee.model

import io.tolgee.model.enums.ApiScope
import org.hibernate.envers.Audited
import javax.persistence.*

@Suppress("LeakingThis")
@Entity
@Audited
data class Permission(
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
    VIEW(1, arrayOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.SCREENSHOTS_VIEW)),
    TRANSLATE(2, arrayOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.TRANSLATIONS_EDIT, ApiScope.SCREENSHOTS_VIEW)),
    EDIT(
      3,
      arrayOf(
        ApiScope.TRANSLATIONS_VIEW,
        ApiScope.TRANSLATIONS_EDIT,
        ApiScope.KEYS_EDIT,
        ApiScope.SCREENSHOTS_VIEW,
        ApiScope.SCREENSHOTS_UPLOAD,
        ApiScope.SCREENSHOTS_DELETE
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
        ApiScope.SCREENSHOTS_DELETE
      )
    );
  }
}
