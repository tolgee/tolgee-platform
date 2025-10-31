package io.tolgee.model.key

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.SimpleKeyResult
import io.tolgee.events.OnKeyPrePersist
import io.tolgee.events.OnKeyPreRemove
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchVersionedEntity
import io.tolgee.model.dataImport.WithKeyMeta
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.task.TaskKey
import io.tolgee.model.translation.Translation
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.ColumnDefault
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.context.ApplicationEventPublisher
import java.util.Date

@Entity
@ActivityLoggedEntity
@ActivityReturnsExistence
@ActivityEntityDescribingPaths(["namespace"])
@EntityListeners(Key.Companion.KeyListeners::class)
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "namespace_id"),
    Index(columnList = "branch_id"),
  ],
)
class Key(
  @field:NotBlank
  @field:Size(max = 2000)
  @field:Column(length = 2000)
  @ActivityLoggedProp
  @ActivityDescribingProp
  var name: String = "",
) : StandardAuditModel(),
  WithKeyMeta,
  BranchVersionedEntity<Key> {
  @field:NotNull
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  lateinit var project: Project

  @ManyToOne
  @ActivityLoggedProp
  var namespace: Namespace? = null

  // Nullable for backward compatibility: NULL represents default branch for legacy data
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_id")
  @ActivityLoggedProp
  var branch: Branch? = null

  @OneToMany(mappedBy = "key")
  var translations: MutableList<Translation> = mutableListOf()

  @OneToMany(mappedBy = "key", orphanRemoval = true)
  var tasks: MutableList<TaskKey> = mutableListOf()

  @OneToOne(mappedBy = "key", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
  override var keyMeta: KeyMeta? = null

  @OneToMany(mappedBy = "key", orphanRemoval = true)
  var keyScreenshotReferences: MutableList<KeyScreenshotReference> = mutableListOf()

  @ActivityLoggedProp
  @ColumnDefault("false")
  var isPlural: Boolean = false

  @ActivityLoggedProp
  var pluralArgName: String? = null

  @Column(nullable = true)
  var cascadeUpdatedAt: Date? = null

  constructor(
    name: String,
    project: Project,
    translations: MutableList<Translation> = mutableListOf(),
  ) : this(name) {
    this.project = project
    this.translations = translations
  }

  val path: PathDTO
    get() = PathDTO.fromFullPath(name)

  val modifiedAt: Date
    get() = cascadeUpdatedAt ?: updatedAt!!

  companion object {
    @Configurable
    class KeyListeners {
      @Autowired
      lateinit var eventPublisherProvider: ObjectFactory<ApplicationEventPublisher>

      @PrePersist
      fun prePersist(key: Key) {
        eventPublisherProvider.`object`.publishEvent(OnKeyPrePersist(source = this, key))
      }

      @PreRemove
      fun preRemove(key: Key) {
        eventPublisherProvider.`object`.publishEvent(OnKeyPreRemove(source = this, key))
      }
    }
  }

  override fun hashCode(): Int {
    return id.hashCode() * name.hashCode()
  }

  fun toSimpleKey(): SimpleKeyResult {
    return SimpleKeyResult(id, name, namespace?.name)
  }

  override fun resolveKeyId(): Long? = id

  override fun isModified(oldState: Map<String, Any>): Boolean {
    return oldState["isPlural"] != this.isPlural || oldState["pluralArgName"] != this.pluralArgName
  }

  override fun differsInBranchVersion(entity: Key): Boolean {
    return true
  }

  override fun merge(source: Key) {
    this.isPlural = source.isPlural
    this.pluralArgName = source.pluralArgName
    this.translations.forEach { translation ->
      translation.merge(source.translations.find { it.language == translation.language }!!)
    }
    this.keyMeta?.let { meta ->
      source.keyMeta?.let { sourceMeta ->
        meta.merge(sourceMeta)
      }
    }
  }
}
