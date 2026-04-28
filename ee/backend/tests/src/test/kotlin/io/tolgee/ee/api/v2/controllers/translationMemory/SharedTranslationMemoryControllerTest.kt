package io.tolgee.ee.api.v2.controllers.translationMemory

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.translationMemory.CreateSharedTranslationMemoryRequest
import io.tolgee.ee.data.translationMemory.UpdateSharedTranslationMemoryRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class SharedTranslationMemoryControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var translationMemoryProjectRepository: TranslationMemoryProjectRepository

  lateinit var testData: TranslationMemoryTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    testData = TranslationMemoryTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private val orgId get() = testData.projectWithoutTm.organizationOwner.id

  @Test
  fun `creates shared translation memory`() {
    val request =
      CreateSharedTranslationMemoryRequest().apply {
        name = "Marketing TM"
        sourceLanguageTag = "en"
      }
    performAuthPost("/v2/organizations/$orgId/translation-memories", request)
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Marketing TM")
        node("sourceLanguageTag").isEqualTo("en")
        node("type").isEqualTo("SHARED")
      }
  }

  @Test
  fun `does not create when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request =
      CreateSharedTranslationMemoryRequest().apply {
        name = "Marketing TM"
        sourceLanguageTag = "en"
      }
    performAuthPost("/v2/organizations/$orgId/translation-memories", request).andIsBadRequest
  }

  @Test
  fun `updates shared translation memory`() {
    // unassignedSharedTm has no project assignments → source language change is allowed.
    // (Changing the source on an assigned TM is rejected — covered by the next test.)
    val tmId = testData.unassignedSharedTm.id
    val update =
      UpdateSharedTranslationMemoryRequest().apply {
        name = "New Name"
        sourceLanguageTag = "de"
      }
    performAuthPut("/v2/organizations/$orgId/translation-memories/$tmId", update)
      .andIsOk
      .andAssertThatJson {
        node("name").isEqualTo("New Name")
        node("sourceLanguageTag").isEqualTo("de")
      }
  }

  @Test
  fun `rejects base language change when projects are assigned`() {
    val tmId = testData.sharedTm.id
    val update =
      UpdateSharedTranslationMemoryRequest().apply {
        name = testData.sharedTm.name
        sourceLanguageTag = "de"
      }
    performAuthPut("/v2/organizations/$orgId/translation-memories/$tmId", update)
      .andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("cannot_change_tm_base_language_while_assigned")
      }
  }

  @Test
  fun `deletes shared translation memory`() {
    val tmId = testData.unassignedSharedTm.id
    performAuthDelete("/v2/organizations/$orgId/translation-memories/$tmId").andIsOk
  }

  @Test
  fun `lists all translation memories in organization`() {
    // projectTm (PROJECT) + 6 SHARED TMs (sharedTm, sharedTmWithPenalty, sharedTmWithOverride,
    // sharedTmReviewedOnly, unassignedSharedTm, snapshotSourceTm).
    performAuthGet("/v2/organizations/$orgId/translation-memories")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemories").isArray.hasSize(8)
      }
  }

  @Test
  fun `cannot update project-type TM via shared endpoint`() {
    val projectTmId = testData.projectTm.id
    val update =
      UpdateSharedTranslationMemoryRequest().apply {
        name = "Hacked"
        sourceLanguageTag = "en"
      }
    performAuthPut("/v2/organizations/$orgId/translation-memories/$projectTmId", update).andIsBadRequest
  }

  // ---------- Permission tests (org MEMBER cannot CUD, MAINTAINER is covered by the default user) ----------

  @Test
  fun `member cannot create shared TM`() {
    userAccount = testData.orgMember
    val request =
      CreateSharedTranslationMemoryRequest().apply {
        name = "New Shared TM"
        sourceLanguageTag = "en"
      }
    performAuthPost("/v2/organizations/$orgId/translation-memories", request).andIsForbidden
  }

  @Test
  fun `member cannot update shared TM`() {
    userAccount = testData.orgMember
    val update =
      UpdateSharedTranslationMemoryRequest().apply {
        name = "Renamed"
        sourceLanguageTag = "en"
      }
    performAuthPut("/v2/organizations/$orgId/translation-memories/${testData.sharedTm.id}", update).andIsForbidden
  }

  @Test
  fun `member cannot delete shared TM`() {
    userAccount = testData.orgMember
    performAuthDelete("/v2/organizations/$orgId/translation-memories/${testData.sharedTm.id}").andIsForbidden
  }

  @Test
  fun `member can list shared TMs`() {
    userAccount = testData.orgMember
    performAuthGet("/v2/organizations/$orgId/translation-memories").andIsOk
  }

  // ---------- With-stats endpoint ----------

  @Test
  fun `lists translation memories with stats`() {
    performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats")
      .andIsOk
      .andAssertThatJson {
        // projectTm (PROJECT) + 6 SHARED TMs (sharedTm, sharedTmWithPenalty,
        // sharedTmWithOverride, sharedTmReviewedOnly, unassignedSharedTm, snapshotSourceTm).
        node("_embedded.translationMemories").isArray.hasSize(8)
      }
  }

  @Test
  fun `with-stats returns entry count and assigned project names`() {
    val result =
      performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats")
        .andIsOk
        .andReturn()

    val tree =
      com.fasterxml.jackson.databind
        .ObjectMapper()
        .readTree(result.response.contentAsString)
    val tms = tree.at("/_embedded/translationMemories")

    // Find shared TM by name (array order is not guaranteed)
    val sharedTm = (0 until tms.size()).map { tms[it] }.first { it["name"].asText() == "Shared Marketing TM" }
    assertThat(sharedTm["entryCount"].asLong()).isEqualTo(2)
    assertThat(sharedTm["assignedProjectsCount"].asLong()).isEqualTo(1)
    assertThat(sharedTm["assignedProjectNames"][0].asText()).isEqualTo("Project With TM")

    // Unassigned TM
    val unassigned = (0 until tms.size()).map { tms[it] }.first { it["name"].asText() == "Unassigned Shared TM" }
    assertThat(unassigned["entryCount"].asLong()).isEqualTo(0)
    assertThat(unassigned["assignedProjectsCount"].asLong()).isEqualTo(0)
  }

  @Test
  fun `with-stats supports search`() {
    performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats?search=Marketing")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemories").isArray.hasSize(1)
        node("_embedded.translationMemories[0].name").isEqualTo("Shared Marketing TM")
      }
  }

  @Test
  fun `with-stats filters by type SHARED`() {
    performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats?type=SHARED")
      .andIsOk
      .andAssertThatJson {
        // sharedTm, sharedTmWithPenalty, sharedTmWithOverride, sharedTmReviewedOnly,
        // mismatchedBaseSharedTm, unassignedSharedTm, snapshotSourceTm
        node("_embedded.translationMemories").isArray.hasSize(7)
        node("_embedded.translationMemories[0].type").isEqualTo("SHARED")
        node("_embedded.translationMemories[1].type").isEqualTo("SHARED")
        node("_embedded.translationMemories[2].type").isEqualTo("SHARED")
        node("_embedded.translationMemories[3].type").isEqualTo("SHARED")
        node("_embedded.translationMemories[4].type").isEqualTo("SHARED")
        node("_embedded.translationMemories[5].type").isEqualTo("SHARED")
        node("_embedded.translationMemories[6].type").isEqualTo("SHARED")
      }
  }

  @Test
  fun `with-stats filters by type PROJECT`() {
    performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats?type=PROJECT")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemories").isArray.hasSize(1)
        node("_embedded.translationMemories[0].type").isEqualTo("PROJECT")
        node("_embedded.translationMemories[0].name").isEqualTo("Project With TM")
      }
  }

  @Test
  fun `with-stats without type returns all`() {
    performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats")
      .andIsOk
      .andAssertThatJson {
        // projectTm + 6 shared TMs = 7
        node("_embedded.translationMemories").isArray.hasSize(8)
      }
  }

  // ---------- Assigned projects endpoint ----------

  @Test
  fun `lists assigned projects for a TM`() {
    val tmId = testData.sharedTm.id
    performAuthGet("/v2/organizations/$orgId/translation-memories/$tmId/assigned-projects")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.assignedProjects").isArray.hasSize(1)
        node("_embedded.assignedProjects[0].projectName").isEqualTo("Project With TM")
        node("_embedded.assignedProjects[0].readAccess").isEqualTo(true)
        node("_embedded.assignedProjects[0].writeAccess").isEqualTo(true)
      }
  }

  @Test
  fun `assigned projects returns empty for unassigned TM`() {
    val tmId = testData.unassignedSharedTm.id
    performAuthGet("/v2/organizations/$orgId/translation-memories/$tmId/assigned-projects")
      .andIsOk
      .andAssertThatJson {
        // Spring HATEOAS omits _embedded when collection is empty
        node("_embedded.assignedProjects").isAbsent()
      }
  }

  // ---------- Create with project assignment ----------

  @Test
  fun `creates shared TM with assigned projects`() {
    val projectId = testData.projectWithTm.id
    val request =
      CreateSharedTranslationMemoryRequest().apply {
        name = "TM With Projects"
        sourceLanguageTag = "en"
        assignedProjectIds = setOf(projectId)
      }
    performAuthPost("/v2/organizations/$orgId/translation-memories", request)
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("TM With Projects")
      }

    // Verify the assignment was created — get the TM ID from the API response, then
    // check the repository directly (avoids lazy-loading issues with detached entities).
    val createdTmId =
      performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats?search=TM With Projects")
        .andReturn()
        .let {
          com.fasterxml.jackson.databind
            .ObjectMapper()
            .readTree(it.response.contentAsString)
            .at("/_embedded/translationMemories/0/id")
            .asLong()
        }
    val assignments = translationMemoryProjectRepository.findByTranslationMemoryId(createdTmId)
    assertThat(assignments).hasSize(1)
    assertThat(assignments[0].project.id).isEqualTo(projectId)
    assertThat(assignments[0].readAccess).isTrue()
    assertThat(assignments[0].writeAccess).isTrue()
  }

  @Test
  fun `creating TM with project assignment sets priority after existing shared assignments`() {
    val projectId = testData.projectWithTm.id
    val existingSharedMax =
      executeInNewTransaction {
        translationMemoryProjectRepository
          .findByProjectId(projectId)
          .filter { it.translationMemory.type == TranslationMemoryType.SHARED }
          .maxOf { it.priority }
      }

    val request =
      CreateSharedTranslationMemoryRequest().apply {
        name = "Priority Test TM"
        sourceLanguageTag = "en"
        assignedProjectIds = setOf(projectId)
      }
    performAuthPost("/v2/organizations/$orgId/translation-memories", request).andIsOk

    val newPriority =
      executeInNewTransaction {
        translationMemoryProjectRepository
          .findByProjectId(projectId)
          .first { it.translationMemory.name == "Priority Test TM" }
          .priority
      }
    assertThat(newPriority).isNotNull().isGreaterThan(existingSharedMax)
  }

  // ---------- Delete TM with project assignments (ON DELETE CASCADE) ----------

  @Test
  fun `deletes shared TM that has project assignments`() {
    val tmId = testData.sharedTm.id
    // sharedTm is assigned to projectWithTm — delete should cascade
    performAuthDelete("/v2/organizations/$orgId/translation-memories/$tmId").andIsOk

    // Verify the assignment was also deleted
    val assignments = translationMemoryProjectRepository.findByTranslationMemoryId(tmId)
    assertThat(assignments).isEmpty()
  }
}
