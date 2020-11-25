package io.polygloat.model

import io.polygloat.constants.ApiScope
import javax.persistence.*

@Entity
data class Permission(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @ManyToOne
        var user: UserAccount? = null,

        @OneToOne
        var invitation: Invitation? = null,

        @Enumerated(EnumType.STRING)
        var type: RepositoryPermissionType? = null
) : AuditModel() {

    constructor(id: Long?, user: UserAccount?, invitation: Invitation?, repository: Repository?,
                type: RepositoryPermissionType?) : this(id, user, invitation, type) {
        this.repository = repository;
    }

    enum class RepositoryPermissionType(val power: Int, val availableScopes: Array<ApiScope>) {
        VIEW(1, arrayOf<ApiScope>(ApiScope.TRANSLATIONS_VIEW)),
        TRANSLATE(2, arrayOf<ApiScope>(ApiScope.TRANSLATIONS_VIEW, ApiScope.TRANSLATIONS_EDIT)),
        EDIT(3, arrayOf<ApiScope>(ApiScope.TRANSLATIONS_VIEW, ApiScope.TRANSLATIONS_EDIT, ApiScope.SOURCES_EDIT)),
        MANAGE(4, arrayOf<ApiScope>(ApiScope.TRANSLATIONS_VIEW, ApiScope.TRANSLATIONS_EDIT, ApiScope.SOURCES_EDIT));
    }

    @ManyToOne
    var repository: Repository? = null

    class PermissionBuilder internal constructor() {
        private var id: Long? = null
        private var user: UserAccount? = null
        private var invitation: Invitation? = null
        private var repository: Repository? = null
        private var type: RepositoryPermissionType? = null
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

        fun repository(repository: Repository?): PermissionBuilder {
            this.repository = repository
            return this
        }

        fun type(type: RepositoryPermissionType?): PermissionBuilder {
            this.type = type
            return this
        }

        fun build(): Permission {
            return Permission(id, user, invitation, repository, type)
        }

        override fun toString(): String {
            return "Permission.PermissionBuilder(id=" + id + ", user=" + user + ", invitation=" + invitation + ", repository=" + repository + ", type=" + type + ")"
        }
    }

    companion object {
        @JvmStatic
        fun builder(): PermissionBuilder {
            return PermissionBuilder()
        }
    }
}