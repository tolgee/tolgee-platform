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
import io.tolgee.model.branching.snapshot.KeyScreenshotReferenceView
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.dataImport.WithKeyMeta
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.task.TaskKey
import io.tolgee.model.translation.Translation
import io.tolgee.service.branching.chooseThreeWay
import io.tolgee.service.branching.isConflictingThreeWay
import io.tolgee.service.branching.mergeByKey
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
  BranchVersionedEntity<Key, KeySnapshot> {
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

  override fun resolveKey(): Key? = this

  override fun isModified(oldState: Map<String, Any>): Boolean {
    return oldState["isPlural"] != this.isPlural || oldState["pluralArgName"] != this.pluralArgName
  }

  override fun hasChanged(snapshot: KeySnapshot): Boolean {
    val changed =
      this.name != snapshot.name || this.isPlural != snapshot.isPlural || this.pluralArgName != snapshot.pluralArgName
    if (changed) {
      return true
    }
    this.keyMeta?.let { meta ->
      snapshot.keyMetaSnapshot?.let { keyMetaSnapshot ->
        if (meta.hasChanged(keyMetaSnapshot)) {
          return true
        }
      }
    }
    if (this.translations.size != snapshot.translations.size) {
      return true
    }
    this.translations.forEach { translation ->
      if (translation.hasChanged(snapshot.translations.find { it.language == translation.language.tag }!!)) {
        return true
      }
    }
    if (this.toScreenshotViews().toSet() != snapshot.screenshotReferences.toSet()) {
      return true
    }
    return false
  }

  override fun isConflicting(
    source: Key,
    snapshot: KeySnapshot,
  ): Boolean {
    if (isConflictingThreeWay(source.isPlural, this.isPlural, snapshot.isPlural)) {
      return true
    }
    if (isConflictingThreeWay(source.pluralArgName, this.pluralArgName, snapshot.pluralArgName)) {
      return true
    }

    source.keyMeta?.let { sourceKeyMeta ->
      snapshot.keyMetaSnapshot?.let { snapshotKeyMeta ->
        if (this.keyMeta?.isConflicting(sourceKeyMeta, snapshotKeyMeta) == true) {
          return true
        }
      }
    }

    val snapshotTranslations = snapshot.translations.associateBy { it.language }
    val targetTranslations = this.translations.associateBy { it.language.tag }

    source.translations.forEach { sourceTranslation ->
      val languageTag = sourceTranslation.language.tag
      val targetTranslation = targetTranslations[languageTag] ?: return@forEach
      val snapshotTranslation = snapshotTranslations[languageTag] ?: return@forEach
      if (targetTranslation.isConflicting(sourceTranslation, snapshotTranslation)) {
        return true
      }
    }

    return false
  }

  override fun merge(
    source: Key,
    snapshot: KeySnapshot?,
    resolution: BranchKeyMergeResolutionType,
  ) {
    this.isPlural = chooseThreeWay(source.isPlural, this.isPlural, snapshot?.isPlural, resolution) ?: false
    this.pluralArgName = chooseThreeWay(source.pluralArgName, this.pluralArgName, snapshot?.pluralArgName, resolution)

    val snapshotTranslations = snapshot?.translations?.associateBy { it.language } ?: emptyMap()
    val targetTranslations = this.translations.associateBy { it.language.tag }
    source.translations.forEach { sourceTranslation ->
      val languageTag = sourceTranslation.language.tag
      val targetTranslation = targetTranslations[languageTag] ?: return@forEach
      val translationSnapshot = snapshotTranslations[languageTag]
      targetTranslation.merge(sourceTranslation, translationSnapshot, resolution)
    }

    this.keyMeta?.let { meta ->
      source.keyMeta?.let { sourceMeta ->
        meta.merge(sourceMeta, snapshot?.keyMetaSnapshot, resolution)
      }
    }

    mergeScreenshots(source, snapshot, resolution)
  }

  private fun mergeScreenshots(
    source: Key,
    snapshot: KeySnapshot?,
    resolution: BranchKeyMergeResolutionType,
  ) {
    val snapshotMap = snapshot?.screenshotReferences?.associateBy { it.screenshotId } ?: emptyMap()
    val sourceMap = source.toScreenshotViews().associateBy { it.screenshotId }
    val targetMap = this.toScreenshotViews().associateBy { it.screenshotId }

    val finalViews = mergeByKey(snapshotMap, sourceMap, targetMap, resolution)

    val targetById = this.keyScreenshotReferences.associateBy { it.screenshot.id }.toMutableMap()
    val sourceById = source.keyScreenshotReferences.associateBy { it.screenshot.id }

    // remove references not present anymore
    this.keyScreenshotReferences.removeIf { it.screenshot.id !in finalViews.keys }

    finalViews.forEach { (id, view) ->
      val existing = targetById[id]
      if (existing != null) {
        existing.positions = view.positions?.toMutableList()
        existing.originalText = view.originalText
      } else {
        val sourceReference = sourceById[id] ?: return@forEach
        val newReference =
          KeyScreenshotReference().apply {
            key = this@Key
            screenshot = sourceReference.screenshot
            positions = view.positions?.toMutableList()
            originalText = view.originalText
          }
        this.keyScreenshotReferences.add(newReference)
      }
    }
  }

  private fun toScreenshotViews(): List<KeyScreenshotReferenceView> =
    this.keyScreenshotReferences.map {
      KeyScreenshotReferenceView(
        screenshotId = it.screenshot.id,
        positions = it.positions?.toList(),
        originalText = it.originalText,
      )
    }
}
