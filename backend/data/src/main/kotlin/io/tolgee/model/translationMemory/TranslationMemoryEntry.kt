package io.tolgee.model.translationMemory

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
  // Note: no `@Index(columnList = "translation_memory_id")` — the composite
  // ix_tm_entry_tm_source (translation_memory_id, md5(source_text)) already covers
  // tm_id-only lookups via its leftmost column.
  indexes = [
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

  companion object {
    /** Max length (in characters) of `source_text` and `target_text`. */
    const val MAX_TEXT_LENGTH = 10_000
  }
}
