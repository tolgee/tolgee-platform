package io.tolgee.model.translationMemory

import io.tolgee.model.translation.Translation
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.io.Serializable

/**
 * Associates a [TranslationMemoryEntry] with the [Translation]s that contributed to it. Populated
 * only for synced (`is_manual = false`) entries — a single synced entry can carry translations
 * from multiple keys across the same project (or, for shared TMs, across multiple assigned
 * projects) once they share the same `(sourceText, targetText, targetLanguageTag)` triple.
 *
 * Manual entries (`is_manual = true`) never appear here. The translation pipeline only writes to
 * `is_manual = false` rows, and manual entries are isolated from the synced pool by design.
 *
 * Key names for the content browser are reached via `entry -> source -> translation -> key`.
 * Deleting a translation cascades to its source rows, which in turn may leave an entry orphaned
 * (no remaining sources); the write pipeline garbage-collects orphans on its next pass.
 */
@Entity
@Table(name = "translation_memory_entry_source")
class TranslationMemoryEntrySource {
  @EmbeddedId
  lateinit var id: TranslationMemoryEntrySourceId

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("entryId")
  @JoinColumn(name = "entry_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  lateinit var entry: TranslationMemoryEntry

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("translationId")
  @JoinColumn(name = "translation_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  lateinit var translation: Translation
}

@Embeddable
class TranslationMemoryEntrySourceId() : Serializable {
  @Column(name = "entry_id")
  var entryId: Long = 0

  @Column(name = "translation_id")
  var translationId: Long = 0

  constructor(entryId: Long, translationId: Long) : this() {
    this.entryId = entryId
    this.translationId = translationId
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TranslationMemoryEntrySourceId) return false
    return entryId == other.entryId && translationId == other.translationId
  }

  override fun hashCode(): Int = 31 * entryId.hashCode() + translationId.hashCode()
}
