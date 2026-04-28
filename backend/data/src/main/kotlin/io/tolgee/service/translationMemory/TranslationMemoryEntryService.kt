package io.tolgee.service.translationMemory

import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.model.translationMemory.TranslationMemoryEntrySource
import io.tolgee.model.translationMemory.TranslationMemoryEntrySourceId
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryEntrySourceRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TranslationMemoryEntryService(
  private val translationMemoryEntryRepository: TranslationMemoryEntryRepository,
  private val translationMemoryEntrySourceRepository: TranslationMemoryEntrySourceRepository,
  private val translationMemoryRepository: TranslationMemoryRepository,
  private val entityManager: EntityManager,
) {
  @set:Autowired
  @set:Lazy
  lateinit var translationMemoryManagementService: TranslationMemoryManagementService

  /**
   * Called after a translation is saved. Maintains the synced entries in every assigned **shared**
   * TM so they reflect the translation's current `(sourceText, targetText, targetLanguageTag)`.
   *
   * Project TMs receive no stored entries — their content is computed virtually from the project's
   * translations at read time — so this method intentionally ignores PROJECT-type assignments.
   *
   * Guards (applied before any IO):
   * - The project has no writable shared assignments → skip (the common free-plan case).
   * - The key lives on a non-default branch → skip (branch merges re-save translations, which
   *   re-enters this hook with the default-branch key).
   * - The saved translation *is* the base language → skip (we track target → base, not vice versa).
   *
   * The per-TM sync is delegated to [syncSharedTmEntry].
   */
  @Transactional
  fun onTranslationSaved(translation: Translation) {
    val key = translation.key
    val projectId = key.project.id

    val writableAssignments = translationMemoryManagementService.getWritableTmAssignments(projectId)
    if (writableAssignments.isEmpty()) return

    val sharedAssignments =
      writableAssignments.filter { it.translationMemory.type != TranslationMemoryType.PROJECT }
    if (sharedAssignments.isEmpty()) return

    val branch = key.branch
    if (branch != null && !branch.isDefault) return

    val baseLanguage = key.project.baseLanguage ?: return
    if (translation.language.id == baseLanguage.id) return

    val baseTranslation = key.translations.firstOrNull { it.language.id == baseLanguage.id }
    val sourceText = baseTranslation?.text
    val targetText = translation.text

    for (assignment in sharedAssignments) {
      syncSharedTmEntry(assignment.translationMemory, translation, sourceText, targetText)
    }
  }

  /**
   * Reconciles one translation's contribution to a single shared TM. Cases:
   *
   * 1. **Nothing changes**: the translation is already linked to a synced entry whose
   *    `(source, target, lang)` still matches — no-op. This is the hot path when a state
   *    transition (e.g. REVIEWED → TRANSLATED) leaves text untouched; it also avoids a
   *    pointless detach + re-create cycle that would otherwise risk FK violations.
   * 2. **Translation should no longer contribute** (empty text, or reviewed-only mismatch, or
   *    source text missing): detach from the current entry, GC the entry if it becomes orphan.
   * 3. **Translation should contribute but to a different entry** (text changed, or currently
   *    unlinked): detach from any stale entry, find-or-create the canonical entry for the
   *    current triple in this TM, attach the translation via [TranslationMemoryEntrySource].
   */
  private fun syncSharedTmEntry(
    tm: TranslationMemory,
    translation: Translation,
    sourceText: String?,
    targetText: String?,
  ) {
    val currentEntry =
      translationMemoryEntrySourceRepository.findEntryByTranslationIdAndTranslationMemoryId(
        translationId = translation.id,
        translationMemoryId = tm.id,
      )

    val shouldWrite =
      !targetText.isNullOrEmpty() &&
        !sourceText.isNullOrEmpty() &&
        (!tm.writeOnlyReviewed || translation.state == TranslationState.REVIEWED)

    // Case 1: current link is still valid — nothing to do.
    if (shouldWrite && currentEntry != null &&
      currentEntry.sourceText == sourceText &&
      currentEntry.targetText == targetText &&
      currentEntry.targetLanguageTag == translation.language.tag
    ) {
      return
    }

    // At this point we either need to detach (maybe only), or detach then re-attach to a
    // different entry. Detach first.
    if (currentEntry != null) {
      detachAndGcIfOrphan(currentEntry, translation.id)
    }

    if (!shouldWrite) return

    val canonical =
      findOrCreateSyncedEntry(
        tm = tm,
        sourceText = sourceText!!,
        targetText = targetText!!,
        targetLanguageTag = translation.language.tag,
      )

    val source =
      TranslationMemoryEntrySource().apply {
        id = TranslationMemoryEntrySourceId(canonical.id, translation.id)
        entry = canonical
        this.translation = translation
      }
    try {
      translationMemoryEntrySourceRepository.save(source)
    } catch (_: DataIntegrityViolationException) {
      // Idempotent: concurrent saves of the same translation racing into the same canonical entry
      // can collide on the (entry_id, translation_id) primary key. Either winner is correct.
    }
  }

  private fun detachAndGcIfOrphan(
    entry: TranslationMemoryEntry,
    translationId: Long,
  ) {
    translationMemoryEntrySourceRepository.deleteByEntryIdAndTranslationId(
      entryId = entry.id,
      translationId = translationId,
    )
    // Flush so the bulk delete is visible to the orphan check below.
    entityManager.flush()

    if (entry.isManual) return
    if (translationMemoryEntrySourceRepository.existsByEntryId(entry.id)) return
    // Orphaned synced entry — no translations still point at it. Manual entries are never GC'd here
    // because the user owns them.
    translationMemoryEntryRepository.delete(entry)
    // Flush so the subsequent find-or-create (if any) doesn't race on a FK to the removed row.
    entityManager.flush()
  }

  private fun findOrCreateSyncedEntry(
    tm: TranslationMemory,
    sourceText: String,
    targetText: String,
    targetLanguageTag: String,
  ): TranslationMemoryEntry {
    translationMemoryEntryRepository
      .findByTranslationMemoryIdAndSourceTextAndTargetTextAndTargetLanguageTagAndIsManual(
        translationMemoryId = tm.id,
        sourceText = sourceText,
        targetText = targetText,
        targetLanguageTag = targetLanguageTag,
        isManual = false,
      )?.let { return it }

    val entry =
      TranslationMemoryEntry().apply {
        translationMemory = translationMemoryRepository.getReferenceById(tm.id)
        this.sourceText = sourceText
        this.targetText = targetText
        this.targetLanguageTag = targetLanguageTag
        isManual = false
      }
    return translationMemoryEntryRepository.save(entry)
  }
}
