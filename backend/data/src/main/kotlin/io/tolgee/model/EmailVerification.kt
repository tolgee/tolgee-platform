package io.tolgee.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

@Entity
@Table(uniqueConstraints = [])

data class EmailVerification(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  @NotBlank
  var code: String? = null,

  @Email
  var newEmail: String? = null
) : AuditModel() {
  @Suppress("JoinDeclarationAndAssignment")
  @OneToOne(optional = false)
  lateinit var userAccount: UserAccount

  constructor(
    id: Long? = null,
    @NotBlank code: String,
    userAccount: UserAccount,
    newEmail: String? = null
  ) :
    this(id = id, code = code, newEmail = newEmail) {
      this.userAccount = userAccount
    }
}
