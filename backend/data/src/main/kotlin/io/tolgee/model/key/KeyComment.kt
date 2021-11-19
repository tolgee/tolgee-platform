package io.tolgee.model.key

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
class KeyComment(
  @ManyToOne(optional = false)
  override var keyMeta: KeyMeta,

  @field:NotNull
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var author: UserAccount? = null
) : StandardAuditModel(), WithKeyMetaReference {

  var fromImport: Boolean = false

  @field:NotBlank
  @Column(columnDefinition = "text", length = 2000)
  var text: String = ""

  override fun toString(): String {
    return "KeyComment(text='$text', fromImport=$fromImport)"
  }
}
