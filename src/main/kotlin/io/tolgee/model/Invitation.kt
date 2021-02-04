package io.tolgee.model

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["code"], name = "invitation_code_unique")])
data class Invitation(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,
        var code: @NotBlank String? = null
) : AuditModel() {

    @OneToOne(mappedBy = "invitation", cascade = [CascadeType.ALL])
    var permission: Permission? = null

    constructor(id: Long?, @NotBlank code: String?, permission: Permission?) : this(id = id, code = code) {
        this.permission = permission
    }
}