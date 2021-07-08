package io.tolgee.model.key

import com.sun.istack.NotNull
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank

@Entity
class KeyCodeReference(
  @ManyToOne(optional = false)
  override var keyMeta: KeyMeta,

  @field:NotNull
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var author: UserAccount? = null,
) : StandardAuditModel(), WithKeyMetaReference {
  @field:NotBlank
  @Column(length = 300)
  var path: String = ""

  var line: Long? = null

  var fromImport: Boolean = false

  override fun toString(): String {
    return "KeyCodeReference(path='$path', line=$line)"
  }
}
