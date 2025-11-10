package io.tolgee.model.key

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Entity
@Table(
  indexes = [
    Index(columnList = "key_meta_id"),
    Index(columnList = "author_id"),
  ],
)
class KeyCodeReference(
  @ManyToOne(optional = false)
  override var keyMeta: KeyMeta,
  @field:NotNull
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var author: UserAccount? = null,
) : StandardAuditModel(),
  WithKeyMetaReference {
  @field:NotBlank
  @Column(length = 300)
  var path: String = ""

  var line: Long? = null

  var fromImport: Boolean = false

  override fun toString(): String {
    return "KeyCodeReference(path='$path', line=$line)"
  }
}
