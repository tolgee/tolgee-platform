package io.tolgee.model

import io.tolgee.constants.ApiScope
import org.hibernate.envers.Audited
import javax.persistence.*

@Entity
@Audited
data class Permission(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @ManyToOne
        var user: UserAccount? = null,

        @OneToOne
        var invitation: Invitation? = null,

        @Enumerated(EnumType.STRING)
        var type: ProjectPermissionType? = null
) : AuditModel() {

    constructor(id: Long? = null, user: UserAccount? = null, invitation: Invitation? = null, project: Project?,
                type: ProjectPermissionType?) : this(id, user, invitation, type) {
        this.project = project
    }

    enum class ProjectPermissionType(val power: Int, val availableScopes: Array<ApiScope>) {
        VIEW(1, arrayOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.SCREENSHOTS_VIEW)),
        TRANSLATE(2, arrayOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.TRANSLATIONS_EDIT, ApiScope.SCREENSHOTS_VIEW)),
        EDIT(3, arrayOf(
                ApiScope.TRANSLATIONS_VIEW,
                ApiScope.TRANSLATIONS_EDIT,
                ApiScope.KEYS_EDIT,
                ApiScope.SCREENSHOTS_VIEW,
                ApiScope.SCREENSHOTS_UPLOAD,
                ApiScope.SCREENSHOTS_DELETE
        )),
        MANAGE(4, arrayOf(
                ApiScope.TRANSLATIONS_VIEW,
                ApiScope.TRANSLATIONS_EDIT,
                ApiScope.KEYS_EDIT,
                ApiScope.SCREENSHOTS_VIEW,
                ApiScope.SCREENSHOTS_UPLOAD,
                ApiScope.SCREENSHOTS_DELETE
        ));
    }

    @ManyToOne
    var project: Project? = null

    class PermissionBuilder internal constructor() {
        private var id: Long? = null
        private var user: UserAccount? = null
        private var invitation: Invitation? = null
        private var project: Project? = null
        private var type: ProjectPermissionType? = null
        fun id(id: Long?): PermissionBuilder {
            this.id = id
            return this
        }

        fun user(user: UserAccount?): PermissionBuilder {
            this.user = user
            return this
        }

        fun invitation(invitation: Invitation?): PermissionBuilder {
            this.invitation = invitation
            return this
        }

        fun project(project: Project?): PermissionBuilder {
            this.project = project
            return this
        }

        fun type(type: ProjectPermissionType?): PermissionBuilder {
            this.type = type
            return this
        }

        fun build(): Permission {
            return Permission(id, user, invitation, project, type)
        }

        override fun toString(): String {
            return "Permission.PermissionBuilder(id=" + id + ", user=" + user + ", invitation=" + invitation + ", project=" + project + ", type=" + type + ")"
        }
    }

    companion object {
        @JvmStatic
        fun builder(): PermissionBuilder {
            return PermissionBuilder()
        }
    }
}
