package io.tolgee.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["code"], name = "invitation_code_unique"),
  ],
)
class Invitation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,
  var code: @NotBlank String,
) : AuditModel() {
  @OneToOne(mappedBy = "invitation", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, optional = true)
  var permission: Permission? = null

  @OneToOne(mappedBy = "invitation", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, optional = true)
  var organizationRole: OrganizationRole? = null

  constructor(
    id: Long?,
    @NotBlank code: String,
    permission: Permission?,
  ) : this(id = id, code = code) {
    this.permission = permission
  }

  var name: String? = null

  var email: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Invitation

    if (id != other.id) return false
    if (code != other.code) return false
    if (name != other.name) return false
    return email == other.email
  }

  override fun hashCode(): Int {
    var result = id?.hashCode() ?: 0
    result = 31 * result + code.hashCode()
    result = 31 * result + (name?.hashCode() ?: 0)
    result = 31 * result + (email?.hashCode() ?: 0)
    return result
  }
}
