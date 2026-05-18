package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Message
import io.tolgee.ee.data.translationMemory.CreateMultipleTranslationMemoryEntriesRequest
import io.tolgee.ee.data.translationMemory.TranslationMemoryEntryRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Mutations on individual entries inside a Translation Memory: create (single + bulk), update,
 * delete (single + per-source-text group + batch).
 *
 * Read-side pagination + counts live on [TranslationMemoryRowListingService]; TMX export lives
 * on [io.tolgee.ee.service.translationMemory.TranslationMemoryTmxService].
 */
@Service
class TranslationMemoryEntryManagementService(
  private val translationMemoryRepository: TranslationMemoryRepository,
  private val translationMemoryEntryRepository: TranslationMemoryEntryRepository,
) {
  fun getEntry(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
  ): TranslationMemoryEntry {
    requireTmInOrganization(organizationId, translationMemoryId)
    val entry =
      translationMemoryEntryRepository.findById(entryId).orElseThrow {
        NotFoundException(Message.TRANSLATION_MEMORY_ENTRY_NOT_FOUND)
      }
    if (entry.translationMemory.id != translationMemoryId) {
      throw NotFoundException(Message.TRANSLATION_MEMORY_ENTRY_NOT_FOUND)
    }
    return entry
  }

  @Transactional
  fun create(
    organizationId: Long,
    translationMemoryId: Long,
    dto: TranslationMemoryEntryRequest,
  ): TranslationMemoryEntry {
    val tm = requireTmInOrganization(organizationId, translationMemoryId)
    val entry =
      TranslationMemoryEntry().apply {
        this.translationMemory = tm
        this.sourceText = dto.sourceText
        this.targetText = dto.targetText
        this.targetLanguageTag = dto.targetLanguageTag
      }
    return translationMemoryEntryRepository.save(entry)
  }

  /**
   * Creates one entry per requested target language, all in one transaction. Replaces the
   * UI's previous per-language POST loop — which could leave the TM in a partially-saved state
   * if a later language failed.
   */
  @Transactional
  fun createMultiple(
    organizationId: Long,
    translationMemoryId: Long,
    dto: CreateMultipleTranslationMemoryEntriesRequest,
  ): List<TranslationMemoryEntry> {
    val tm = requireTmInOrganization(organizationId, translationMemoryId)
    val duplicateLangs =
      dto.translations
        .groupingBy { it.targetLanguageTag }
        .eachCount()
        .filterValues { it > 1 }
        .keys
    if (duplicateLangs.isNotEmpty()) {
      throw BadRequestException(
        Message.TRANSLATION_MEMORY_ENTRY_DUPLICATE_TARGET_LANGUAGE,
        listOf(duplicateLangs.sorted().joinToString(", ")),
      )
    }
    val entries =
      dto.translations.map { translation ->
        TranslationMemoryEntry().apply {
          this.translationMemory = tm
          this.sourceText = dto.sourceText
          this.targetText = translation.targetText
          this.targetLanguageTag = translation.targetLanguageTag
        }
      }
    return translationMemoryEntryRepository.saveAll(entries)
  }

  @Transactional
  fun update(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
    dto: TranslationMemoryEntryRequest,
  ): TranslationMemoryEntry {
    val entry = getEntry(organizationId, translationMemoryId, entryId)
    entry.sourceText = dto.sourceText
    entry.targetText = dto.targetText
    entry.targetLanguageTag = dto.targetLanguageTag
    return translationMemoryEntryRepository.save(entry)
  }

  @Transactional
  fun delete(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
  ) {
    val entry = getEntry(organizationId, translationMemoryId, entryId)
    translationMemoryEntryRepository.delete(entry)
  }

  /** Deletes every stored entry sharing the source text of [entryId]. */
  @Transactional
  fun deleteGroup(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
  ): Int {
    val entry = getEntry(organizationId, translationMemoryId, entryId)
    return translationMemoryEntryRepository.deleteByTranslationMemoryIdAndSourceText(
      translationMemoryId = translationMemoryId,
      sourceText = entry.sourceText,
    )
  }

  /** Batch variant of [deleteGroup]. Dedupes to distinct source_texts. */
  @Transactional
  fun deleteMultipleGroups(
    organizationId: Long,
    translationMemoryId: Long,
    entryIds: Set<Long>,
  ): Int {
    requireTmInOrganization(organizationId, translationMemoryId)
    if (entryIds.isEmpty()) return 0
    val entries =
      translationMemoryEntryRepository
        .findAllById(entryIds)
        .filter { it.translationMemory.id == translationMemoryId }
    if (entries.isEmpty()) return 0
    val sourceTexts = entries.map { it.sourceText }.toSet()
    var totalDeleted = 0
    for (sourceText in sourceTexts) {
      totalDeleted +=
        translationMemoryEntryRepository.deleteByTranslationMemoryIdAndSourceText(
          translationMemoryId = translationMemoryId,
          sourceText = sourceText,
        )
    }
    return totalDeleted
  }

  private fun requireTmInOrganization(
    organizationId: Long,
    translationMemoryId: Long,
  ): TranslationMemory {
    return translationMemoryRepository.find(organizationId, translationMemoryId)
      ?: throw NotFoundException(Message.TRANSLATION_MEMORY_NOT_FOUND)
  }
}
