package io.tolgee.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotBlank

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["code"], name = "invitation_code_unique")
  ]
)
class Invitation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,
  var code: @NotBlank String
) : AuditModel() {

  @OneToOne(mappedBy = "invitation", cascade = [CascadeType.ALL])
  var permission: Permission? = null

  @OneToOne(mappedBy = "invitation", cascade = [CascadeType.ALL])
  var organizationRole: OrganizationRole? = null

  constructor(id: Long?, @NotBlank code: String, permission: Permission?) : this(id = id, code = code) {
    this.permission = permission
  }

  var name: String? = null

  var email: String? = null
}
