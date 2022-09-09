package io.tolgee.model

import java.util.*
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Temporal
import javax.persistence.TemporalType
import javax.persistence.Transient
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["tokenHash"], name = "pat_token_hash_unique")])
class Pat(
  @field:NotEmpty
  @field:NotNull
  var tokenHash: String = "",

  @field:NotEmpty
  @field:NotNull
  var description: String = "",

  @Temporal(value = TemporalType.TIMESTAMP)
  var expiresAt: Date? = null,

  @Temporal(value = TemporalType.TIMESTAMP)
  var lastUsedAt: Date? = null,

  @Transient
  var token: String? = null
) : StandardAuditModel() {

  @ManyToOne
  @NotNull
  lateinit var userAccount: UserAccount
}
