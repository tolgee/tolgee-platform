package io.tolgee.model.key

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.propChangesProvider.TagsPropChangesProvider
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.BranchVersionedEntity
import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.service.branching.chooseThreeWay
import io.tolgee.service.branching.mergeSetsWithBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Type

@Entity
@EntityListeners(KeyMeta.Companion.KeyMetaListener::class)
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["key"])
class KeyMeta(
  @OneToOne
  var key: Key? = null,
  @OneToOne
  var importKey: ImportKey? = null,
) : StandardAuditModel(),
  BranchVersionedEntity<KeyMeta, KeyMetaSnapshot> {
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

  @ActivityLoggedProp
  @Column(columnDefinition = "text")
  @Size(max = DESCRIPTION_MAX_LEN)
  var description: String? = null

  @ActivityLoggedProp
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var custom: MutableMap<String, Any?>? = null

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

  fun setCustom(
    key: String,
    value: Any?,
  ) {
    val custom =
      custom ?: mutableMapOf<String, Any?>()
        .also {
          custom = it
        }
    custom[key] = value
  }

  companion object {
    const val DESCRIPTION_MAX_LEN = 2000

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

  override fun resolveKey(): Key? = key

  override fun isModified(oldState: Map<String, Any>): Boolean {
    return oldState["description"] != this.description || oldState["custom"] != this.custom
  }

  override fun hasChanged(snapshot: KeyMetaSnapshot): Boolean {
    return this.description != snapshot.description || this.custom != snapshot.custom || this.tags != snapshot.tags
  }

  override fun isConflicting(
    source: KeyMeta,
    snapshot: KeyMetaSnapshot,
  ): Boolean {
    if (source.description != this.description && source.description != snapshot.description) return true
    if (source.custom != this.custom && source.custom != snapshot.custom) return true
    return false
  }

  override fun merge(
    source: KeyMeta,
    snapshot: KeyMetaSnapshot?,
    resolution: BranchKeyMergeResolutionType,
  ) {
    this.description = chooseThreeWay(source.description, this.description, snapshot?.description, resolution)
    this.custom = chooseThreeWay(source.custom, this.custom, snapshot?.custom, resolution)

    val snapshotTags = snapshot?.tags?.toSet().orEmpty()
    val sourceTags = source.tags.toSet()
    val targetTags = this.tags.toSet()
    val finalTags = mergeSetsWithBase(snapshotTags, sourceTags, targetTags)

    this.tags.clear()
    this.tags.addAll(finalTags)
  }
}
