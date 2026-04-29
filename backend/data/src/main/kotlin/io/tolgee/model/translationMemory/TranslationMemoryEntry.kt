package io.tolgee.model.translationMemory

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
  indexes = [
    Index(columnList = "translation_memory_id"),
    Index(columnList = "target_language_tag"),
  ],
)
class TranslationMemoryEntry : StandardAuditModel() {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  lateinit var translationMemory: TranslationMemory

  @Column(columnDefinition = "text", nullable = false)
  var sourceText: String = ""

  @Column(columnDefinition = "text", nullable = false)
  var targetText: String = ""

  @Column(nullable = false)
  var targetLanguageTag: String = ""

  @Column(columnDefinition = "text", nullable = true)
  var tuid: String? = null

  /**
   * Manual entries (created via the "Add entry" dialog or a TMX import) are user-owned and
   * editable. Synced entries (`false`) are created by [TranslationMemoryEntryService.onTranslationSaved]
   * from an assigned project's translations and are read-only in the content browser.
   *
   * The two origins are intentionally kept as separate rows even when the `(sourceText, targetText,
   * targetLanguageTag)` triple matches — see [TranslationMemoryEntrySource] and the write pipeline
   * for the dedup semantics applied within each origin.
   */
  @Column(nullable = false)
  @ColumnDefault("true")
  var isManual: Boolean = true
}
