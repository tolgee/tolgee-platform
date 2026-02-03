package io.tolgee.model

import io.tolgee.model.enums.Scope
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.Date

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["keyHash"], name = "api_key_hash_unique"),
    UniqueConstraint(columnNames = ["key"], name = "api_key_unique"),
  ],
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "user_account_id"),
  ],
)
class ApiKey(
  @Column(updatable = false, insertable = false, nullable = true)
  var key: String? = null,
  /**
   * Scope should be never nullable, but here were entries with null scopes in the production DB, which caused NPEs,
   * so to be sure, lets make it nullable
   */
  @NotNull
  @NotEmpty
  @Enumerated(EnumType.STRING)
  @ElementCollection(targetClass = Scope::class, fetch = FetchType.EAGER)
  var scopesEnum: MutableSet<Scope?>,
) : StandardAuditModel() {
  @NotBlank
  var description: String = ""

  @NotBlank
  var keyHash: String = ""

  /**
   * Encoded key with project id
   */
  @Transient
  var encodedKey: String? = null

  @ManyToOne
  @NotNull
  lateinit var userAccount: UserAccount

  @ManyToOne
  @NotNull
  lateinit var project: Project

  @Temporal(TemporalType.TIMESTAMP)
  var expiresAt: Date? = null

  @Temporal(TemporalType.TIMESTAMP)
  var lastUsedAt: Date? = null

  constructor(
    key: String,
    scopesEnum: Set<Scope>,
    userAccount: UserAccount,
    project: Project,
  ) : this(key, scopesEnum.toMutableSet()) {
    this.userAccount = userAccount
    this.project = project
  }
}
