package io.tolgee.model

import io.tolgee.constants.ApiScope
import java.util.*
import java.util.stream.Collectors
import javax.persistence.*
import javax.validation.constraints.NotBlank
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
    var key: String? = null
) : AuditModel() {

    @ManyToOne
    @NotNull
    var userAccount: UserAccount? = null

    @ManyToOne
    @NotNull
    var repository: Repository? = null

    @NotBlank
    @NotNull
    private var scopes: String? = null

    constructor(
        id: Long?,
        @NotNull
        userAccount: UserAccount?,

        @NotNull
        repository: Repository?,

        @NotEmpty @NotNull
        key: String?,

        @NotBlank @NotNull
        scopes:  String?
    ) : this(id, key) {
        this.userAccount = userAccount
        this.repository = repository
        this.scopes = scopes
    }

    fun getScopesSet(): Set<ApiScope> {
        return Arrays.stream(scopes!!.split(";".toRegex()).toTypedArray())
            .map { value: String? -> ApiScope.fromValue(value) }
            .collect(Collectors.toSet())
    }

    fun setScopesSet(scopes: Set<ApiScope>) {
        this.scopes = stringifyScopes(scopes)
    }

    class ApiKeyBuilder internal constructor() {
        private var scopes: String? = null
        private var id: Long? = null
        private var userAccount: @NotNull UserAccount? = null
        private var repository: @NotNull Repository? = null
        private var key: @NotEmpty @NotNull String? = null
        fun scopes(scopes: Set<ApiScope>): ApiKeyBuilder {
            this.scopes = stringifyScopes(scopes)
            return this
        }

        fun id(id: Long?): ApiKeyBuilder {
            this.id = id
            return this
        }

        fun userAccount(userAccount: @NotNull UserAccount?): ApiKeyBuilder {
            this.userAccount = userAccount
            return this
        }

        fun repository(repository: @NotNull Repository?): ApiKeyBuilder {
            this.repository = repository
            return this
        }

        fun key(key: @NotEmpty @NotNull String?): ApiKeyBuilder {
            this.key = key
            return this
        }

        fun build(): ApiKey {
            return ApiKey(id, userAccount, repository, key, scopes)
        }

        override fun toString(): String {
            return "ApiKey.ApiKeyBuilder(scopes=" + scopes + ", id=" + id + ", userAccount=" + userAccount + ", repository=" + repository + ", key=" + key + ")"
        }
    }

    companion object {
        @JvmStatic
        fun builder(): ApiKeyBuilder {
            return ApiKeyBuilder()
        }

        private fun stringifyScopes(scopes: Set<ApiScope>): String {
            return scopes.stream().map { obj: ApiScope -> obj.value }.collect(Collectors.joining(";"))
        }
    }
}