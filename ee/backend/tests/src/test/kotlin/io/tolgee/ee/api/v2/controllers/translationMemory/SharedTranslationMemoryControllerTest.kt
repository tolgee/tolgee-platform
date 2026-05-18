package io.tolgee.ee.api.v2.controllers.translationMemory

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.translationMemory.ProjectAssignmentDto
import io.tolgee.ee.data.translationMemory.SharedTranslationMemoryRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
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
      SharedTranslationMemoryRequest().apply {
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
      SharedTranslationMemoryRequest().apply {
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
      SharedTranslationMemoryRequest().apply {
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
      SharedTranslationMemoryRequest().apply {
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
    // 4 PROJECT TMs (projectTm + onlyProjectTm built explicitly, plus auto-created for
    // projectWithoutTm and conflictProject) + 7 SHARED TMs (sharedTm, sharedTmWithPenalty,
    // sharedTmWithOverride, sharedTmReviewedOnly, unassignedSharedTm, mismatchedBaseSharedTm,
    // multiProjectSharedTm).
    performAuthGet("/v2/organizations/$orgId/translation-memories")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemories").isArray.hasSize(11)
      }
  }

  @Test
  fun `rejects update on project-type TM`() {
    val projectTmId = testData.projectTm.id
    val update =
      SharedTranslationMemoryRequest().apply {
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
      SharedTranslationMemoryRequest().apply {
        name = "New Shared TM"
        sourceLanguageTag = "en"
      }
    performAuthPost("/v2/organizations/$orgId/translation-memories", request).andIsForbidden
  }

  @Test
  fun `member cannot update shared TM`() {
    userAccount = testData.orgMember
    val update =
      SharedTranslationMemoryRequest().apply {
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

  @Test
  fun `project-only viewer cannot list org TMs`() {
    // Project-level access via direct permission on projectWithTm — no org role. The org-scoped
    // TM list would otherwise leak cross-project content (other projects' TMs, virtual rows
    // from projects the viewer doesn't belong to).
    userAccount = testData.projectOnlyViewer
    performAuthGet("/v2/organizations/$orgId/translation-memories").andIsForbidden
    performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats").andIsForbidden
    performAuthGet("/v2/organizations/$orgId/translation-memories/${testData.sharedTm.id}").andIsForbidden
  }

  // ---------- With-stats endpoint ----------

  @Test
  fun `lists translation memories with stats`() {
    performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats")
      .andIsOk
      .andAssertThatJson {
        // 4 PROJECT TMs (projectTm + onlyProjectTm + auto-created for projectWithoutTm +
        // auto-created for conflictProject) + 7 SHARED TMs.
        node("_embedded.translationMemories").isArray.hasSize(11)
      }
  }

  @Test
  fun `with-stats returns assigned project names`() {
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
    assertThat(sharedTm["assignedProjectNames"].size()).isEqualTo(1)
    assertThat(sharedTm["assignedProjectNames"][0].asText()).isEqualTo("Project With TM")

    // Unassigned TM
    val unassigned = (0 until tms.size()).map { tms[it] }.first { it["name"].asText() == "Unassigned Shared TM" }
    assertThat(unassigned["assignedProjectNames"].size()).isEqualTo(0)
  }

  @Test
  fun `entry-counts returns counts for requested ids and omits unknown ones`() {
    val sharedTm = testData.sharedTm
    val unassigned = testData.unassignedSharedTm
    val bogusId = 999_999_999L

    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/entry-counts?ids=${sharedTm.id}&ids=${unassigned.id}&ids=$bogusId",
    ).andIsOk.andAssertThatJson {
      node("counts.${sharedTm.id}").isEqualTo(2)
      node("counts.${unassigned.id}").isEqualTo(0)
      node("counts.$bogusId").isAbsent()
    }
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
        // mismatchedBaseSharedTm, unassignedSharedTm, multiProjectSharedTm
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
        // projectTm + onlyProjectTm (both built explicitly) + auto-created TMs for
        // projectWithoutTm and conflictProject.
        node("_embedded.translationMemories").isArray.hasSize(4)
        node("_embedded.translationMemories[0].type").isEqualTo("PROJECT")
        node("_embedded.translationMemories[1].type").isEqualTo("PROJECT")
        node("_embedded.translationMemories[2].type").isEqualTo("PROJECT")
        node("_embedded.translationMemories[3].type").isEqualTo("PROJECT")
      }
  }

  @Test
  fun `with-stats without type returns all`() {
    performAuthGet("/v2/organizations/$orgId/translation-memories-with-stats")
      .andIsOk
      .andAssertThatJson {
        // 4 PROJECT TMs + 7 SHARED TMs.
        node("_embedded.translationMemories").isArray.hasSize(11)
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
        node("_embedded.assignedProjects[0].writeAccess").isEqualTo(false)
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
      SharedTranslationMemoryRequest().apply {
        name = "TM With Projects"
        sourceLanguageTag = "en"
        assignedProjects = listOf(ProjectAssignmentDto().apply { this.projectId = projectId })
      }
    val createResult =
      performAuthPost("/v2/organizations/$orgId/translation-memories", request)
        .andIsOk
        .andAssertThatJson {
          node("id").isValidId
          node("name").isEqualTo("TM With Projects")
        }.andReturn()

    // Verify the assignment was created — pull the TM id straight from the POST response so
    // the lookup doesn't depend on search-endpoint semantics or the name being globally unique.
    val createdTmId =
      ObjectMapper().readTree(createResult.response.contentAsString).path("id").asLong()
    val assignments = translationMemoryProjectRepository.findByTranslationMemoryId(createdTmId)
    assertThat(assignments).hasSize(1)
    assertThat(assignments[0].project.id).isEqualTo(projectId)
    assertThat(assignments[0].readAccess).isTrue()
    assertThat(assignments[0].writeAccess).isTrue()
  }

  @Test
  fun `creating TM with project assignment sets priority after existing shared assignments`() {
    // testData seeds projectWithTm with project TM at 0 and shared TMs at priorities 1..5
    // (sharedTm=1, sharedTmWithPenalty=2, sharedTmWithOverride=3, sharedTmReviewedOnly=4,
    // multiProjectSharedTm=5). A new assignment uses max+1 → 6.
    val projectId = testData.projectWithTm.id
    val request =
      SharedTranslationMemoryRequest().apply {
        name = "Priority Test TM"
        sourceLanguageTag = "en"
        assignedProjects = listOf(ProjectAssignmentDto().apply { this.projectId = projectId })
      }
    performAuthPost("/v2/organizations/$orgId/translation-memories", request).andIsOk

    val newPriority =
      executeInNewTransaction {
        translationMemoryProjectRepository
          .findByProjectId(projectId)
          .first { it.translationMemory.name == "Priority Test TM" }
          .priority
      }
    assertThat(newPriority).isEqualTo(6)
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
