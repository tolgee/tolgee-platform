package io.polygloat.model

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(uniqueConstraints = [])
data class EmailVerification(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,
        @NotBlank
        var code: String? = null,
) : AuditModel() {
    @Suppress("JoinDeclarationAndAssignment")
    @OneToOne(optional = false)
    lateinit var userAccount: UserAccount

    constructor(id: Long? = null, @NotBlank code: String, userAccount: UserAccount) : this(id = id, code = code) {
        this.userAccount = userAccount
    }
}