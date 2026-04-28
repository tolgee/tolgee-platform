package io.tolgee.service.translationMemory

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryEntrySourceRepository
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import io.tolgee.service.project.ProjectCreationService
import io.tolgee.service.project.ProjectHardDeletingService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class TranslationMemoryServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var translationMemoryProjectRepository: TranslationMemoryProjectRepository

  @Autowired
  lateinit var translationMemoryEntryRepository: TranslationMemoryEntryRepository

  @Autowired
  lateinit var translationMemoryEntrySourceRepository: TranslationMemoryEntrySourceRepository

  @Autowired
  lateinit var translationMemoryRepository: TranslationMemoryRepository

  @Autowired
  lateinit var translationMemoryManagementService: TranslationMemoryManagementService

  @Autowired
  lateinit var projectCreationService: ProjectCreationService

  @Autowired
  lateinit var projectHardDeletingService: ProjectHardDeletingService

  @Autowired
  lateinit var managedTranslationMemorySuggestionService: ManagedTranslationMemorySuggestionService

  lateinit var testData: TranslationMemoryTestData

  @BeforeEach
  fun setup() {
    testData = TranslationMemoryTestData()
    testDataService.saveTestData(testData.root)
  }

  /** Key names contributing to a synced entry — via the source join table. */
  private fun keyNamesFor(entry: TranslationMemoryEntry): List<String> =
    translationMemoryEntrySourceRepository.findKeyNamesByEntryId(entry.id)

  private fun syncedEntriesIn(tmId: Long): List<TranslationMemoryEntry> =
    translationMemoryEntryRepository
      .findByTranslationMemoryId(tmId)
      .filter { !it.isManual }

  @Test
  fun `project creation auto-creates project TM`() {
    val orgId = testData.projectWithoutTm.organizationOwner.id

    val project =
      projectCreationService.createProject(
        CreateProjectRequest(
          name = "New TM Project",
          languages = listOf(LanguageRequest(name = "English", originalName = "English", tag = "en")),
          organizationId = orgId,
        ),
      )

    val assignments = translationMemoryProjectRepository.findByProjectId(project.id)
    assertThat(assignments).hasSize(1)

    val assignment = assignments.first()
    assertThat(assignment.readAccess).isTrue()
    assertThat(assignment.writeAccess).isTrue()
    // Project TM lands at the top (priority 0); shared TMs stack under it via max+1.
    assertThat(assignment.priority).isEqualTo(0)

    val tm = assignment.translationMemory
    assertThat(tm.type).isEqualTo(TranslationMemoryType.PROJECT)
    assertThat(tm.sourceLanguageTag).isEqualTo("en")
    assertThat(tm.name).isEqualTo("New TM Project")
    assertThat(tm.organizationOwner.id).isEqualTo(orgId)

    // Project TMs are pure config — no entries are materialized.
    assertThat(translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)).isEmpty()
  }

  @Test
  fun `write pipeline skips for projects without TM`() {
    val project = projectService.get(testData.projectWithoutTm.id)
    val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()

    val key = keyService.create(project, CreateKeyDto("no-tm-key", null, mapOf("en" to "Hello")))

    val translation = translationService.getOrCreate(key, german)
    translation.text = "Hallo"
    translationService.save(translation)

    // projectWithoutTm has no TM assignments, so nothing materializes anywhere.
    val allSyncedForKey =
      translationMemoryEntryRepository
        .findAll()
        .filter { !it.isManual }
        .filter { keyNamesFor(it).contains("no-tm-key") }
    assertThat(allSyncedForKey).isEmpty()
  }

  @Test
  fun `write pipeline creates synced entry in shared TMs and leaves project TM untouched`() {
    val project = projectService.get(testData.projectWithTm.id)
    val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()

    val key = keyService.create(project, CreateKeyDto("new-key", null, mapOf("en" to "New text")))

    val translation = translationService.getOrCreate(key, german)
    translation.text = "Neuer Text"
    translationService.save(translation)

    // Project TM receives no stored entries under the virtual-content model.
    assertThat(syncedEntriesIn(testData.projectTm.id)).isEmpty()

    // Each assigned shared TM gets a synced entry linked to the saved translation.
    val sharedEntry =
      syncedEntriesIn(testData.sharedTm.id).firstOrNull {
        keyNamesFor(it).contains("new-key") && it.targetLanguageTag == "de"
      }
    assertThat(sharedEntry).isNotNull()
    assertThat(sharedEntry!!.sourceText).isEqualTo("New text")
    assertThat(sharedEntry.targetText).isEqualTo("Neuer Text")
  }

  @Test
  fun `write pipeline skips keys on non-default branches`() {
    val german = languageService.findEntitiesByTags(setOf("de"), testData.projectWithTm.id).first()
    val key = keyService.get(testData.keyOnFeatureBranch.id)

    val translation = translationService.getOrCreate(key, german)
    translation.text = "Entwurf"
    translationService.save(translation)

    // No shared TM should have picked this up — feature branches don't feed the TM.
    val leaked =
      syncedEntriesIn(testData.sharedTm.id).firstOrNull {
        keyNamesFor(it).contains(testData.keyOnFeatureBranch.name)
      }
    assertThat(leaked).isNull()
  }

  @Test
  fun `updating translation reuses the same entry across saves`() {
    // testData.existingTargetTranslation is "Bestehende Übersetzung" in German under "existing-key".
    // First save materializes a synced entry in each shared TM; second save with a different text
    // detaches from the first entry, (GC'd as orphan) and re-links to a fresh one. Either way, at
    // most one entry per shared TM for this key.
    val translation = translationService.get(testData.existingTargetTranslation.id)

    translation.text = "Bestehende Übersetzung"
    translationService.save(translation)

    translation.text = "Aktualisierte Übersetzung"
    translationService.save(translation)

    val sharedEntries =
      syncedEntriesIn(testData.sharedTm.id).filter { keyNamesFor(it).contains("existing-key") }
    assertThat(sharedEntries).hasSize(1)
    assertThat(sharedEntries.first().targetText).isEqualTo("Aktualisierte Übersetzung")
  }

  @Test
  fun `deleting translation removes its source link from synced entries`() {
    // Needs real commits for the FK cascade to fire against the DB rather than sit inside
    // Hibernate's session cache, so everything runs in its own transaction.
    val ids =
      executeInNewTransaction {
        val project = projectService.get(testData.projectWithTm.id)
        val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()
        val key = keyService.create(project, CreateKeyDto("delete-me-key", null, mapOf("en" to "Delete me")))
        val translation = translationService.getOrCreate(key, german).apply { text = "Lösch mich" }
        val saved = translationService.save(translation)
        saved.id
      }
    val translationId = ids

    val entryIdBefore =
      executeInNewTransaction {
        syncedEntriesIn(testData.sharedTm.id)
          .first { keyNamesFor(it).contains("delete-me-key") }
          .id
      }

    executeInNewTransaction {
      translationService.deleteByIdIn(listOf(translationId))
    }

    executeInNewTransaction {
      val translationCount =
        entityManager
          .createNativeQuery("select count(*) from translation where id = :id")
          .setParameter("id", translationId)
          .singleResult as Number
      assertThat(translationCount.toLong()).`as`("translation row").isEqualTo(0L)

      val remainingSources =
        entityManager
          .createNativeQuery(
            "select count(*) from translation_memory_entry_source " +
              "where entry_id = :entryId and translation_id = :translationId",
          ).setParameter("entryId", entryIdBefore)
          .setParameter("translationId", translationId)
          .singleResult as Number
      assertThat(remainingSources.toLong()).`as`("source row").isEqualTo(0L)
    }
  }

  @Test
  fun `project deletion cleans up TM data`() {
    val projectId = testData.projectWithTm.id
    val tmId = testData.projectTm.id

    executeInNewTransaction {
      val project = projectService.get(projectId)
      projectHardDeletingService.hardDeleteProject(project)
    }

    // TM assignments should be gone
    assertThat(translationMemoryProjectRepository.findByProjectId(projectId)).isEmpty()

    // The project TM itself should be deleted
    assertThat(translationMemoryRepository.findById(tmId)).isEmpty()
  }

  @Test
  fun `getOrCreateProjectTm returns existing TM without duplicating`() {
    val project = projectService.get(testData.projectWithTm.id)
    val existingTmId = testData.projectTm.id

    val tm1 = translationMemoryManagementService.getOrCreateProjectTm(project)
    val tm2 = translationMemoryManagementService.getOrCreateProjectTm(project)

    assertThat(tm1.id).isEqualTo(existingTmId)
    assertThat(tm2.id).isEqualTo(existingTmId)

    val projectTms =
      translationMemoryProjectRepository
        .findByProjectId(project.id)
        .map { it.translationMemory }
        .filter { it.type == TranslationMemoryType.PROJECT }
    assertThat(projectTms).hasSize(1)
  }

  @Test
  fun `getOrCreateProjectTm creates the config row when missing`() {
    val project = projectService.get(testData.projectWithoutTm.id)

    val tm = translationMemoryManagementService.getOrCreateProjectTm(project)

    assertThat(tm.type).isEqualTo(TranslationMemoryType.PROJECT)
    // No entries — project TM content is virtual now.
    assertThat(translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)).isEmpty()
  }

  @Test
  fun `base language change updates project TM sourceLanguageTag without touching entries`() {
    // Unassign all shared TMs so the base-language-match validation doesn't block the change.
    translationMemoryProjectRepository.deleteAll(
      translationMemoryProjectRepository
        .findByProjectId(testData.projectWithTm.id)
        .filter { it.translationMemory.type != TranslationMemoryType.PROJECT },
    )
    entityManager.flush()

    val project = projectService.get(testData.projectWithTm.id)
    val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()

    projectService.editProject(
      project.id,
      EditProjectRequest(
        name = project.name,
        slug = project.slug,
        baseLanguageId = german.id,
        useNamespaces = project.useNamespaces,
        useBranching = project.useBranching,
        defaultNamespaceId = null,
        description = project.description,
        icuPlaceholders = project.icuPlaceholders,
        suggestionsMode = project.suggestionsMode,
        translationProtection = project.translationProtection,
      ),
    )
    entityManager.flush()
    entityManager.clear()

    val refreshedTm = translationMemoryRepository.findById(testData.projectTm.id).orElseThrow()
    assertThat(refreshedTm.sourceLanguageTag).isEqualTo("de")
    // No rebuild — project TM content is virtual, recomputed from translations at read time.
    assertThat(translationMemoryEntryRepository.findByTranslationMemoryId(testData.projectTm.id))
      .isEmpty()
  }

  @Test
  fun `project rename syncs project TM name`() {
    val project = projectService.get(testData.projectWithTm.id)
    val originalName = testData.projectTm.name
    assertThat(originalName).isEqualTo(project.name)

    projectService.editProject(
      project.id,
      EditProjectRequest(
        name = "Renamed Project",
        slug = project.slug,
        baseLanguageId = project.baseLanguage?.id,
        useNamespaces = project.useNamespaces,
        useBranching = project.useBranching,
        defaultNamespaceId = null,
        description = project.description,
        icuPlaceholders = project.icuPlaceholders,
        suggestionsMode = project.suggestionsMode,
        translationProtection = project.translationProtection,
      ),
    )
    entityManager.flush()
    entityManager.clear()

    val refreshedTm = translationMemoryRepository.findById(testData.projectTm.id).orElseThrow()
    assertThat(refreshedTm.name).isEqualTo("Renamed Project")
  }

  @Test
  fun `reviewed-only TM skips TRANSLATED saves while permissive siblings take them`() {
    val project = projectService.get(testData.projectWithTm.id)
    val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()

    val key = keyService.create(project, CreateKeyDto("fresh-key", null, mapOf("en" to "Fresh source")))
    val translation = translationService.getOrCreate(key, german)
    translation.text = "Frischer Text" // stays TRANSLATED
    translationService.save(translation)

    assertThat(
      syncedEntriesIn(testData.sharedTmReviewedOnly.id).flatMap { keyNamesFor(it) },
    ).doesNotContain("fresh-key")

    assertThat(
      syncedEntriesIn(testData.sharedTm.id).flatMap { keyNamesFor(it) },
    ).contains("fresh-key")
  }

  @Test
  fun `reviewed-only TM accepts REVIEWED saves`() {
    val project = projectService.get(testData.projectWithTm.id)
    val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()

    val key = keyService.create(project, CreateKeyDto("reviewed-on-save", null, mapOf("en" to "Reviewed source 2")))
    val translation = translationService.getOrCreate(key, german)
    translation.text = "Überprüfter Text"
    translation.state = TranslationState.REVIEWED
    translationService.save(translation)

    val match =
      syncedEntriesIn(testData.sharedTmReviewedOnly.id)
        .firstOrNull { keyNamesFor(it).contains("reviewed-on-save") }
    assertThat(match).isNotNull()
    assertThat(match!!.targetText).isEqualTo("Überprüfter Text")
  }

  @Test
  fun `promoting TRANSLATED to REVIEWED adds the entry to a reviewed-only TM`() {
    val project = projectService.get(testData.projectWithTm.id)
    val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()

    val key = keyService.create(project, CreateKeyDto("promoted-key", null, mapOf("en" to "Promoted source")))
    val translation = translationService.getOrCreate(key, german)
    translation.text = "Hochgestufter Text"
    translationService.save(translation)

    assertThat(
      syncedEntriesIn(testData.sharedTmReviewedOnly.id).flatMap { keyNamesFor(it) },
    ).doesNotContain("promoted-key")

    translation.state = TranslationState.REVIEWED
    translationService.save(translation)

    val after =
      syncedEntriesIn(testData.sharedTmReviewedOnly.id)
        .firstOrNull { keyNamesFor(it).contains("promoted-key") }
    assertThat(after).isNotNull()
    assertThat(after!!.targetText).isEqualTo("Hochgestufter Text")
  }

  @Test
  fun `demoting REVIEWED to TRANSLATED removes entry only from reviewed-only TMs`() {
    val project = projectService.get(testData.projectWithTm.id)
    val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()

    val key = keyService.create(project, CreateKeyDto("demotion-key", null, mapOf("en" to "Demotion source")))
    val translation = translationService.getOrCreate(key, german)
    translation.text = "Herabzustufender Text"
    translation.state = TranslationState.REVIEWED
    translationService.save(translation)
    entityManager.flush()

    assertThat(
      syncedEntriesIn(testData.sharedTmReviewedOnly.id).flatMap { keyNamesFor(it) },
    ).contains("demotion-key")

    translation.state = TranslationState.TRANSLATED
    translationService.save(translation)
    entityManager.flush()

    assertThat(
      syncedEntriesIn(testData.sharedTmReviewedOnly.id).flatMap { keyNamesFor(it) },
    ).doesNotContain("demotion-key")

    assertThat(
      syncedEntriesIn(testData.sharedTm.id).flatMap { keyNamesFor(it) },
    ).contains("demotion-key")
  }

  @Test
  fun `project base language change is rejected when shared TMs have mismatched source`() {
    val project = projectService.get(testData.projectWithTm.id)
    val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()

    val exception =
      org.junit.jupiter.api.Assertions.assertThrows(io.tolgee.exceptions.BadRequestException::class.java) {
        projectService.editProject(
          project.id,
          EditProjectRequest(
            name = project.name,
            slug = project.slug,
            baseLanguageId = german.id,
            useNamespaces = project.useNamespaces,
            useBranching = project.useBranching,
            defaultNamespaceId = null,
            description = project.description,
            icuPlaceholders = project.icuPlaceholders,
            suggestionsMode = project.suggestionsMode,
            translationProtection = project.translationProtection,
          ),
        )
      }
    assertThat(exception.code).isEqualTo("cannot_change_project_base_language_tm_conflict")
    val conflictNames =
      (exception.params ?: emptyList())
        .filterIsInstance<Map<*, *>>()
        .mapNotNull { it["name"] as? String }
    assertThat(conflictNames).contains(
      "Shared Marketing TM",
      "Shared TM with default penalty",
      "Shared TM with per-assignment penalty override",
      "Reviewed-only shared TM",
    )
  }

  @Test
  fun `managed getSuggestionsList returns virtual project TM entries for MT context`() {
    // Free-plan (CE default): getReadableTmIdsForSuggestions returns only the project TM, and
    // the project TM's content is virtual (from translations). The fixture has a translation
    // "Existing source" -> "Bestehende Übersetzung" (de) under existing-key — that pair should
    // surface as a suggestion without ever being materialized as an entry row.
    val project = projectService.get(testData.projectWithTm.id)

    val results =
      managedTranslationMemorySuggestionService.getSuggestionsList(
        baseTranslationText = "Existing source",
        isPlural = false,
        keyId = null,
        projectId = project.id,
        organizationId = project.organizationOwner.id,
        targetLanguageTag = "de",
        limit = 5,
      )

    assertThat(results).isNotEmpty
    val match = results.first { it.baseTranslationText == "Existing source" }
    assertThat(match.targetTranslationText).isEqualTo("Bestehende Übersetzung")
    // Project TM never has a penalty applied — similarity == rawSimilarity.
    assertThat(match.similarity).isEqualTo(match.rawSimilarity)
  }
}
