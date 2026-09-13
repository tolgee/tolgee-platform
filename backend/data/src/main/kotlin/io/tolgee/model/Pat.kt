package io.tolgee.model

import io.tolgee.security.PAT_PREFIX
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.Date

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["tokenHash"],
      name = "pat_token_hash_unique",
    ),
  ],
  indexes = [
    Index(columnList = "user_account_id"),
  ],
)
class Pat(
  @param:NotEmpty
  @param:NotNull
  var tokenHash: String = "",
  @param:NotEmpty
  @param:NotNull
  var description: String = "",
  var expiresAt: Date? = null,
  var lastUsedAt: Date? = null,
  @Transient
  var token: String? = null,
) : StandardAuditModel() {
  @ManyToOne
  @NotNull
  lateinit var userAccount: UserAccount

  val tokenWithPrefix: String?
    get() = token?.let { "$PAT_PREFIX$token" }
}
