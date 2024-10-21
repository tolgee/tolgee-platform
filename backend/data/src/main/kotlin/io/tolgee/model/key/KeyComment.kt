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
class KeyComment(
  @ManyToOne(optional = false)
  override var keyMeta: KeyMeta,
  @field:NotNull
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var author: UserAccount? = null,
) : StandardAuditModel(), WithKeyMetaReference {
  var fromImport: Boolean = false

  @field:NotBlank
  @Column(columnDefinition = "text", length = 2000)
  var text: String = ""

  override fun toString(): String {
    return "KeyComment(text='$text', fromImport=$fromImport)"
  }
}
