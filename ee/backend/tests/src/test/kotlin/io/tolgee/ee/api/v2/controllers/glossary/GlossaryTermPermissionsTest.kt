package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryPermissionsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.glossary.CreateGlossaryTermWithTranslationRequest
import io.tolgee.ee.data.glossary.DeleteMultipleGlossaryTermsRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryTermWithTranslationRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class GlossaryTermPermissionsTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: GlossaryPermissionsTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.GLOSSARY)
    testData = GlossaryPermissionsTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `owner can get all glossary terms`() {
    userAccount = testData.userOwner
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.glossaryTerms") {
          isArray.hasSize(1)
          node("[0].id").isValidId
          node("[0].description").isEqualTo("The description")
        }
      }
  }

  @Test
  fun `maintainer can get all glossary terms`() {
    userAccount = testData.userMaintainer
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.glossaryTerms") {
          isArray.hasSize(1)
          node("[0].id").isValidId
          node("[0].description").isEqualTo("The description")
        }
      }
  }

  @Test
  fun `member can get all glossary terms`() {
    userAccount = testData.userMember
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.glossaryTerms") {
          isArray.hasSize(1)
          node("[0].id").isValidId
          node("[0].description").isEqualTo("The description")
        }
      }
  }

  @Test
  fun `unaffiliated cannot get all glossary terms`() {
    userAccount = testData.userUnaffiliated
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms")
      .andIsNotFound
  }

  @Test
  fun `owner can get single glossary term`() {
    userAccount = testData.userOwner
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("description").isEqualTo("The description")
      }
  }

  @Test
  fun `maintainer can get single glossary term`() {
    userAccount = testData.userMaintainer
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("description").isEqualTo("The description")
      }
  }

  @Test
  fun `member can get single glossary term`() {
    userAccount = testData.userMember
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("description").isEqualTo("The description")
      }
  }

  @Test
  fun `unaffiliated cannot get single glossary term`() {
    userAccount = testData.userUnaffiliated
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsNotFound
  }

  @Test
  fun `owner can create glossary term`() {
    userAccount = testData.userOwner
    val request =
      CreateGlossaryTermWithTranslationRequest().apply {
        description = "New Term"
        text = "New Translation"
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsOk
      .andAssertThatJson {
        node("term.id").isValidId
        node("term.description").isEqualTo("New Term")
        node("translation.text").isEqualTo("New Translation")
      }
  }

  @Test
  fun `maintainer can create glossary term`() {
    userAccount = testData.userMaintainer
    val request =
      CreateGlossaryTermWithTranslationRequest().apply {
        description = "New Term"
        text = "New Translation"
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsOk
      .andAssertThatJson {
        node("term.id").isValidId
        node("term.description").isEqualTo("New Term")
        node("translation.text").isEqualTo("New Translation")
      }
  }

  @Test
  fun `member cannot create glossary term`() {
    userAccount = testData.userMember
    val request =
      CreateGlossaryTermWithTranslationRequest().apply {
        description = "New Term"
        text = "New Translation"
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsForbidden
  }

  @Test
  fun `unaffiliated cannot create glossary term`() {
    userAccount = testData.userUnaffiliated
    val request =
      CreateGlossaryTermWithTranslationRequest().apply {
        description = "New Term"
        text = "New Translation"
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsNotFound
  }

  @Test
  fun `owner can update glossary term`() {
    userAccount = testData.userOwner
    val request =
      UpdateGlossaryTermWithTranslationRequest().apply {
        description = "Updated Term"
        text = "Updated Translation"
      }
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
      request,
    ).andIsOk
      .andAssertThatJson {
        node("term.id").isValidId
        node("term.description").isEqualTo("Updated Term")
        node("translation.text").isEqualTo("Updated Translation")
      }
  }

  @Test
  fun `maintainer can update glossary term`() {
    userAccount = testData.userMaintainer
    val request =
      UpdateGlossaryTermWithTranslationRequest().apply {
        description = "Updated Term"
        text = "Updated Translation"
      }
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
      request,
    ).andIsOk
      .andAssertThatJson {
        node("term.id").isValidId
        node("term.description").isEqualTo("Updated Term")
        node("translation.text").isEqualTo("Updated Translation")
      }
  }

  @Test
  fun `member cannot update glossary term`() {
    userAccount = testData.userMember
    val request =
      UpdateGlossaryTermWithTranslationRequest().apply {
        description = "Updated Term"
        text = "Updated Translation"
      }
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
      request,
    ).andIsForbidden
  }

  @Test
  fun `unaffiliated cannot update glossary term`() {
    userAccount = testData.userUnaffiliated
    val request =
      UpdateGlossaryTermWithTranslationRequest().apply {
        description = "Updated Term"
        text = "Updated Translation"
      }
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
      request,
    ).andIsNotFound
  }

  @Test
  fun `owner can delete glossary term`() {
    userAccount = testData.userOwner
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk

    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsNotFound
  }

  @Test
  fun `maintainer can delete glossary term`() {
    userAccount = testData.userMaintainer
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk

    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsNotFound
  }

  @Test
  fun `member cannot delete glossary term`() {
    userAccount = testData.userMember
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsForbidden
  }

  @Test
  fun `unaffiliated cannot delete glossary term`() {
    userAccount = testData.userUnaffiliated
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsNotFound
  }

  @Test
  fun `owner can delete multiple glossary terms`() {
    userAccount = testData.userOwner
    val request =
      DeleteMultipleGlossaryTermsRequest().apply {
        termIds = setOf(testData.term.id)
      }
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsOk

    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsNotFound
  }

  @Test
  fun `maintainer can delete multiple glossary terms`() {
    userAccount = testData.userMaintainer
    val request =
      DeleteMultipleGlossaryTermsRequest().apply {
        termIds = setOf(testData.term.id)
      }
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsOk

    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsNotFound
  }

  @Test
  fun `member cannot delete multiple glossary terms`() {
    userAccount = testData.userMember
    val request =
      DeleteMultipleGlossaryTermsRequest().apply {
        termIds = setOf(testData.term.id)
      }
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsForbidden
  }

  @Test
  fun `unaffiliated cannot delete multiple glossary terms`() {
    userAccount = testData.userUnaffiliated
    val request =
      DeleteMultipleGlossaryTermsRequest().apply {
        termIds = setOf(testData.term.id)
      }
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsNotFound
  }
}
