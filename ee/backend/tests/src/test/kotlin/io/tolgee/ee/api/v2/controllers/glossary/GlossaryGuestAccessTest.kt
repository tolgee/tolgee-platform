package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryGuestAccessTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.service.glossary.GlossaryService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class GlossaryGuestAccessTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var glossaryService: GlossaryService

  lateinit var testData: GlossaryGuestAccessTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.GLOSSARY)
    testData = GlossaryGuestAccessTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `guest lists only glossaries assigned to accessible projects`() {
    assertGuestList(
      testData.virtualGuest,
      "Mixed assignment glossary",
      "Public project glossary",
    )
    assertGuestList(
      testData.storedGuest,
      "Mixed assignment glossary",
      "Public project glossary",
    )
    assertGuestList(
      testData.noneOnlyUser,
      "Mixed assignment glossary",
      "Public project glossary",
    )
  }

  @Test
  fun `guest stats cover only accessible projects of a mixed-assignment glossary`() {
    userAccount = testData.virtualGuest
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries-with-stats?sort=name&sort=id",
    ).andIsOk.andAssertThatJson {
      node("_embedded.glossaries") {
        isArray.hasSize(2)
        node("[0].name").isEqualTo("Mixed assignment glossary")
        node("[0].assignedProjectsCount").isEqualTo(1)
        node("[0].firstAssignedProjectName").isEqualTo("Guest visible public project")
        node("[1].name").isEqualTo("Public project glossary")
      }
    }
  }

  @Test
  fun `guest sees only accessible assigned projects of a mixed-assignment glossary`() {
    userAccount = testData.virtualGuest
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.mixedGlossary.id}/assigned-projects",
    ).andIsOk.andAssertThatJson {
      node("_embedded.projects") {
        isArray.hasSize(1)
        node("[0].name").isEqualTo("Guest visible public project")
      }
    }
  }

  @Test
  fun `direct project permission holder reads glossaries of accessible projects`() {
    assertGuestList(
      testData.directPermissionUser,
      "Mixed assignment glossary",
      "Private project glossary",
      "Public project glossary",
    )
  }

  @Test
  fun `guest can read an accessible glossary but not a members-only one`() {
    userAccount = testData.virtualGuest
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.publicGlossary.id}")
      .andIsOk
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.privateGlossary.id}")
      .andIsNotFound
    userAccount = testData.storedGuest
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.privateGlossary.id}")
      .andIsNotFound
  }

  @Test
  fun `guest terms access follows the glossary accessibility`() {
    userAccount = testData.virtualGuest
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.publicGlossary.id}/terms",
    ).andIsOk
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.privateGlossary.id}/terms",
    ).andIsNotFound
  }

  @Test
  fun `glossaries of soft-deleted projects or base languages stay hidden from guests`() {
    userAccount = testData.virtualGuest
    val base = "/v2/organizations/${testData.organization.id}/glossaries"
    performAuthGet("$base/${testData.softDeletedProjectGlossary.id}").andIsNotFound
    performAuthGet("$base/${testData.softDeletedProjectGlossary.id}/terms").andIsNotFound
    performAuthGet("$base/${testData.softDeletedProjectGlossary.id}/assigned-projects").andIsNotFound
    performAuthGet("$base/${testData.softDeletedBaseLangGlossary.id}").andIsNotFound
    performAuthGet("$base/${testData.softDeletedBaseLangGlossary.id}/terms").andIsNotFound
    performAuthGet("$base/${testData.softDeletedBaseLangGlossary.id}/assigned-projects").andIsNotFound
  }

  @Test
  fun `guest single-term reads follow the glossary accessibility`() {
    userAccount = testData.virtualGuest
    val publicTermPath =
      "/v2/organizations/${testData.organization.id}" +
        "/glossaries/${testData.publicGlossary.id}/terms/${testData.publicGlossaryTerm.id}"
    val privateTermPath =
      "/v2/organizations/${testData.organization.id}" +
        "/glossaries/${testData.privateGlossary.id}/terms/${testData.privateGlossaryTerm.id}"
    performAuthGet(publicTermPath).andIsOk
    performAuthGet("$publicTermPath/translations/en").andIsOk
    performAuthGet(privateTermPath).andIsNotFound
    performAuthGet("$privateTermPath/translations/en").andIsNotFound
  }

  @Test
  fun `an unauthenticated context takes the guest-restricted path`() {
    executeInNewTransaction {
      glossaryService.find(testData.organization.id, testData.privateGlossary.id).assert.isNull()
      glossaryService.find(testData.organization.id, testData.publicGlossary.id).assert.isNotNull()
    }
  }

  @Test
  fun `guest cannot create a glossary`() {
    userAccount = testData.storedGuest
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/glossaries",
      mapOf("name" to "Nope", "baseLanguageTag" to "en"),
    ).andIsForbidden
  }

  @Test
  fun `member sees all glossaries`() {
    userAccount = testData.user
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.glossaries").isArray.hasSize(6)
      }
  }

  @Test
  fun `guest export omits a private co-assigned project's languages`() {
    userAccount = testData.virtualGuest
    exportLanguageHeaders(testData.mixedGlossary.id).assert.doesNotContain("de")

    userAccount = testData.user
    exportLanguageHeaders(testData.mixedGlossary.id).assert.contains("de")
  }

  private fun exportLanguageHeaders(glossaryId: Long): List<String> {
    val csv =
      performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/$glossaryId/export")
        .andIsOk
        .andReturn()
        .response.contentAsString
    val headers = csv.lines()[0].split(",").map { it.trim().removeSurrounding("\"") }
    return headers.drop(6)
  }

  private fun assertGuestList(
    user: UserAccount,
    vararg expectedNames: String,
  ) {
    userAccount = user
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries?sort=name&sort=id")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.glossaries") {
          isArray.hasSize(expectedNames.size)
          expectedNames.forEachIndexed { index, name ->
            node("[$index].name").isEqualTo(name)
          }
        }
      }
  }
}
