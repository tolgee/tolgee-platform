package io.tolgee.model.key

import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.propChangesProvider.TagsPropChangesProvider
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.ImportKey
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate

@Entity
@EntityListeners(KeyMeta.Companion.KeyMetaListener::class)
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["key"])
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
  @ActivityLoggedProp(TagsPropChangesProvider::class)
  var tags: MutableSet<Tag> = mutableSetOf()

  fun addComment(
    author: UserAccount? = null,
    ft: KeyComment.() -> Unit,
  ) {
    KeyComment(this, author).apply {
      ft()
      comments.add(this)
    }
  }

  fun addCodeReference(
    author: UserAccount? = null,
    ft: KeyCodeReference.() -> Unit,
  ) {
    KeyCodeReference(this, author).apply {
      ft()
      codeReferences.add(this)
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
