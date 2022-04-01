package io.tolgee.model.key

import io.tolgee.activity.ActivityLogged
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.ImportKey
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.OrderBy
import javax.persistence.PrePersist
import javax.persistence.PreUpdate

@Entity
@EntityListeners(KeyMeta.Companion.KeyMetaListener::class)
@ActivityLogged
class KeyMeta(
  @OneToOne
  var key: Key? = null,

  @OneToOne
  var importKey: ImportKey? = null,
) : StandardAuditModel() {

  @OneToMany(mappedBy = "keyMeta")
  @OrderBy("id")
  var comments = mutableListOf<KeyComment>()

  @OneToMany(mappedBy = "keyMeta")
  @OrderBy("id")
  var codeReferences = mutableListOf<KeyCodeReference>()

  @ManyToMany
  @OrderBy("id")
  var tags: MutableSet<Tag> = mutableSetOf()

  fun addComment(author: UserAccount? = null, ft: KeyComment.() -> Unit) {
    KeyComment(this, author).apply(ft).also {
      this.comments.add(it)
    }
  }

  fun addCodeReference(author: UserAccount? = null, ft: KeyCodeReference.() -> Unit) {
    KeyCodeReference(this, author).apply(ft).also {
      this.codeReferences.add(it)
    }
  }

  companion object {
    class KeyMetaListener {
      @PrePersist
      @PreUpdate
      fun preSave(keyMeta: KeyMeta) {
        if (!(keyMeta.key == null).xor(keyMeta.importKey == null)) {
          throw Exception("Exactly one of key or importKey must be set!")
        }
      }
    }
  }
}
