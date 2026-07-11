package io.tolgee.service.translationMemory

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import io.tolgee.service.project.ProjectCreationService
import io.tolgee.service.project.ProjectHardDeletingService
import io.tolgee.service.translation.TranslationMemoryService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
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
  lateinit var translationMemoryRepository: TranslationMemoryRepository

  @Autowired
  lateinit var projectCreationService: ProjectCreationService

  @Autowired
  lateinit var projectHardDeletingService: ProjectHardDeletingService

  @Autowired
  lateinit var translationMemoryService: TranslationMemoryService

  lateinit var testData: TranslationMemoryTestData

  @BeforeEach
  fun setup() {
    testData = TranslationMemoryTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

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
    assertThat(assignment.priority).isEqualTo(0)

    val tm = assignment.translationMemory
    assertThat(tm.type).isEqualTo(TranslationMemoryType.PROJECT)
    assertThat(tm.sourceLanguageTag).isEqualTo("en")
    assertThat(tm.name).isEqualTo("New TM Project")
    assertThat(tm.organizationOwner.id).isEqualTo(orgId)

    assertThat(translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)).isEmpty()
  }

  @Test
  fun `project deletion cleans up TM data`() {
    val projectId = testData.projectWithTm.id
    val tmId = testData.projectTm.id

    executeInNewTransaction {
      val project = projectService.get(projectId)
      projectHardDeletingService.hardDeleteProject(project)
    }

    assertThat(translationMemoryProjectRepository.findByProjectId(projectId)).isEmpty()
    assertThat(translationMemoryRepository.findById(tmId)).isEmpty()
  }

  @Test
  fun `base language change updates project TM sourceLanguageTag`() {
    val project = testData.projectWithOnlyProjectTm
    val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()

    executeInNewTransaction {
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
    entityManager.clear()

    val refreshedTm = translationMemoryRepository.findById(testData.onlyProjectTm.id).orElseThrow()
    assertThat(refreshedTm.sourceLanguageTag).isEqualTo("de")
  }

  @Test
  fun `project rename syncs project TM name`() {
    val project = testData.projectWithTm
    assertThat(testData.projectTm.name).isEqualTo(project.name)

    executeInNewTransaction {
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
    }
    entityManager.clear()

    val refreshedTm = translationMemoryRepository.findById(testData.projectTm.id).orElseThrow()
    assertThat(refreshedTm.name).isEqualTo("Renamed Project")
  }

  @Test
  fun `getSuggestionsList returns virtual rows from write-assigned project translations`() {
    val project = testData.projectWithTm

    val results =
      translationMemoryService.getSuggestionsList(
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
  }
}
