package io.tolgee.model

import io.tolgee.constants.ApiScope
import javax.persistence.*
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["key"], name = "api_key_unique")])
data class ApiKey(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @NotEmpty
        @NotNull
        var key: String? = null,

        @NotNull
        @NotEmpty
        @Enumerated(EnumType.ORDINAL)
        @field:ElementCollection(targetClass = ApiScope::class, fetch = FetchType.EAGER)
        var scopesEnum: Set<ApiScope>
) : AuditModel() {

    @ManyToOne
    @NotNull
    var userAccount: UserAccount? = null

    @ManyToOne
    @NotNull
    var project: Project? = null

    @Deprecated(level = DeprecationLevel.WARNING,
            message = "Scopes field is deprecated, it should not persist string values",
            replaceWith = ReplaceWith("scopesEnum")
    )
    private var scopes: String? = null

    constructor(
            id: Long? = null,

            @NotNull
            userAccount: UserAccount?,

            @NotNull
            project: Project?,

            @NotEmpty @NotNull
            key: String?,

            scopes: String? = null,

            @NotEmpty @NotNull
            scopesEnum: Set<ApiScope>
    ) : this(id, key, scopesEnum) {
        this.userAccount = userAccount
        this.project = project
        @Suppress("DEPRECATION")
        this.scopes = scopes
    }
}
