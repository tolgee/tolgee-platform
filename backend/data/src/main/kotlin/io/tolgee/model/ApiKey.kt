package io.tolgee.model

import io.tolgee.model.enums.Scope
import java.util.*
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Temporal
import javax.persistence.TemporalType
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["keyHash"], name = "api_key_hash_unique"),
    UniqueConstraint(columnNames = ["key"], name = "api_key_unique")
  ]
)
class ApiKey(
  @Column(updatable = false, insertable = false, nullable = true)
  var key: String? = null,

  @NotNull
  @NotEmpty
  @Enumerated(EnumType.STRING)
  @field:ElementCollection(targetClass = Scope::class, fetch = FetchType.EAGER)
  var scopesEnum: MutableSet<Scope>
) : StandardAuditModel() {

  @field:NotBlank
  var description: String = ""

  @field:NotBlank
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
    project: Project
  ) : this(key, scopesEnum.toMutableSet()) {
    this.userAccount = userAccount
    this.project = project
  }
}
