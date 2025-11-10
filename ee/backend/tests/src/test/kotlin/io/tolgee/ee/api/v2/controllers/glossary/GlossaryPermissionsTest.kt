package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryPermissionsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.glossary.CreateGlossaryRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryRequest
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
class GlossaryPermissionsTest : AuthorizedControllerTest() {
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
  fun `owner can get all glossaries`() {
    userAccount = testData.userOwner
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.glossaries") {
          isArray.hasSize(1)
          node("[0].id").isValidId
          node("[0].name").isEqualTo("Test Glossary")
        }
      }
  }

  @Test
  fun `maintainer can get all glossaries`() {
    userAccount = testData.userMaintainer
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.glossaries") {
          isArray.hasSize(1)
          node("[0].id").isValidId
          node("[0].name").isEqualTo("Test Glossary")
        }
      }
  }

  @Test
  fun `member can get all glossaries`() {
    userAccount = testData.userMember
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.glossaries") {
          isArray.hasSize(1)
          node("[0].id").isValidId
          node("[0].name").isEqualTo("Test Glossary")
        }
      }
  }

  @Test
  fun `unaffiliated cannot get all glossaries`() {
    userAccount = testData.userUnaffiliated
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries")
      .andIsNotFound
  }

  @Test
  fun `owner can get single glossary`() {
    userAccount = testData.userOwner
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Test Glossary")
        node("baseLanguageTag").isEqualTo("en")
      }
  }

  @Test
  fun `maintainer can get single glossary`() {
    userAccount = testData.userMaintainer
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Test Glossary")
        node("baseLanguageTag").isEqualTo("en")
      }
  }

  @Test
  fun `member can get single glossary`() {
    userAccount = testData.userMember
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Test Glossary")
        node("baseLanguageTag").isEqualTo("en")
      }
  }

  @Test
  fun `unaffiliated cannot get single glossary`() {
    userAccount = testData.userUnaffiliated
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsNotFound
  }

  @Test
  fun `owner can create glossary`() {
    userAccount = testData.userOwner
    val request =
      CreateGlossaryRequest().apply {
        name = "New Glossary"
        baseLanguageTag = "en"
        assignedProjectIds = mutableSetOf(testData.project.id)
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries", request)
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("New Glossary")
        node("baseLanguageTag").isEqualTo("en")
      }
  }

  @Test
  fun `maintainer can create glossary`() {
    userAccount = testData.userMaintainer
    val request =
      CreateGlossaryRequest().apply {
        name = "New Glossary"
        baseLanguageTag = "en"
        assignedProjectIds = mutableSetOf(testData.project.id)
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries", request)
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("New Glossary")
        node("baseLanguageTag").isEqualTo("en")
      }
  }

  @Test
  fun `member cannot create glossary`() {
    userAccount = testData.userMember
    val request =
      CreateGlossaryRequest().apply {
        name = "New Glossary"
        baseLanguageTag = "en"
        assignedProjectIds = mutableSetOf(testData.project.id)
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries", request)
      .andIsForbidden
  }

  @Test
  fun `unaffiliated cannot create glossary`() {
    userAccount = testData.userUnaffiliated
    val request =
      CreateGlossaryRequest().apply {
        name = "New Glossary"
        baseLanguageTag = "en"
        assignedProjectIds = mutableSetOf(testData.project.id)
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries", request)
      .andIsNotFound
  }

  @Test
  fun `owner can update glossary`() {
    userAccount = testData.userOwner
    val request =
      UpdateGlossaryRequest().apply {
        name = "Updated Glossary"
        baseLanguageTag = "de"
      }
    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Updated Glossary")
        node("baseLanguageTag").isEqualTo("de")
      }
  }

  @Test
  fun `maintainer can update glossary`() {
    userAccount = testData.userMaintainer
    val request =
      UpdateGlossaryRequest().apply {
        name = "Updated Glossary"
        baseLanguageTag = "de"
      }
    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Updated Glossary")
        node("baseLanguageTag").isEqualTo("de")
      }
  }

  @Test
  fun `member cannot update glossary`() {
    userAccount = testData.userMember
    val request =
      UpdateGlossaryRequest().apply {
        name = "Updated Glossary"
        baseLanguageTag = "de"
      }
    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsForbidden
  }

  @Test
  fun `unaffiliated cannot update glossary`() {
    userAccount = testData.userUnaffiliated
    val request =
      UpdateGlossaryRequest().apply {
        name = "Updated Glossary"
        baseLanguageTag = "de"
      }
    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsNotFound
  }

  @Test
  fun `owner can delete glossary`() {
    userAccount = testData.userOwner
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk

    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsNotFound
  }

  @Test
  fun `maintainer can delete glossary`() {
    userAccount = testData.userMaintainer
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk

    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsNotFound
  }

  @Test
  fun `member cannot delete glossary`() {
    userAccount = testData.userMember
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsForbidden
  }

  @Test
  fun `unaffiliated cannot delete glossary`() {
    userAccount = testData.userUnaffiliated
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsNotFound
  }
}
