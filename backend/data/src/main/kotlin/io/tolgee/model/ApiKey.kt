package io.tolgee.model

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.propChangesProvider.ValueCollectionPropChangesProvider
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
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.Date

@Entity
@ActivityLoggedEntity
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
  @param:NotNull
  @param:NotEmpty
  @Enumerated(EnumType.STRING)
  @ElementCollection(targetClass = Scope::class, fetch = FetchType.EAGER)
  @ActivityLoggedProp(ValueCollectionPropChangesProvider::class)
  var scopesEnum: MutableSet<Scope?>,
) : StandardAuditModel() {
  @NotBlank
  @ActivityLoggedProp
  @ActivityDescribingProp
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

  @ActivityLoggedProp
  var expiresAt: Date? = null

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
