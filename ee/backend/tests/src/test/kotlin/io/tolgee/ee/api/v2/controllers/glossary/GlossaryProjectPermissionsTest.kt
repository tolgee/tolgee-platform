package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryPermissionsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.glossary.CreateGlossaryRequest
import io.tolgee.ee.data.glossary.CreateGlossaryTermWithTranslationRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryTermWithTranslationRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
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
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class GlossaryProjectPermissionsTest : AuthorizedControllerTest() {
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
  fun `user with project-specific permissions can access glossary highlights`() {
    userAccount = testData.userProjectTranslator
    val text = "This is a Term that should be highlighted"

    performAuthPost(
      "/v2/projects/${testData.project.id}/glossary-highlights",
      mapOf(
        "languageTag" to "en",
        "text" to text,
      ),
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.glossaryHighlights") {
          isArray.hasSize(1)
          node("[0].position.start").isNumber.isEqualTo(BigDecimal(10))
          node("[0].position.end").isNumber.isEqualTo(BigDecimal(14))
          node("[0].value.id").isValidId
          node("[0].value.description").isEqualTo("The description")
        }
      }
  }

  @Test
  fun `user with project-specific permissions can get all glossaries`() {
    userAccount = testData.userProjectTranslator
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
  fun `user with project-specific permissions can get single glossary`() {
    userAccount = testData.userProjectTranslator
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("Test Glossary")
        node("baseLanguageTag").isEqualTo("en")
      }
  }

  @Test
  fun `user with project-specific permissions cannot create glossary`() {
    userAccount = testData.userProjectTranslator
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
  fun `user with project-specific permissions cannot update glossary`() {
    userAccount = testData.userProjectTranslator
    val request =
      UpdateGlossaryRequest().apply {
        name = "Updated Glossary"
        baseLanguageTag = "de"
      }
    performAuthPut("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}", request)
      .andIsForbidden
  }

  @Test
  fun `user with project-specific permissions cannot delete glossary`() {
    userAccount = testData.userProjectTranslator
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}")
      .andIsForbidden
  }

  @Test
  fun `user with project-specific permissions can get all glossary terms`() {
    userAccount = testData.userProjectTranslator
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
  fun `user with project-specific permissions can get single glossary term`() {
    userAccount = testData.userProjectTranslator
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("description").isEqualTo("The description")
      }
  }

  @Test
  fun `user with project-specific permissions cannot create glossary term`() {
    userAccount = testData.userProjectTranslator
    val request =
      CreateGlossaryTermWithTranslationRequest().apply {
        description = "New Term"
        text = "New Translation"
      }
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms",
      request,
    ).andIsForbidden
  }

  @Test
  fun `user with project-specific permissions cannot update glossary term`() {
    userAccount = testData.userProjectTranslator
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
  fun `user with project-specific permissions cannot delete glossary term`() {
    userAccount = testData.userProjectTranslator
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsForbidden
  }
}
