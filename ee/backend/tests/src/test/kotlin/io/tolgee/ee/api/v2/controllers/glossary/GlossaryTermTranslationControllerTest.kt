package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.glossary.UpdateGlossaryTermTranslationRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class GlossaryTermTranslationControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: GlossaryTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.GLOSSARY)
    testData = GlossaryTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.userOwner
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `updates glossary term translation`() {
    val request =
      UpdateGlossaryTermTranslationRequest().apply {
        languageTag = "de"
        text = "Neuer Begriff"
      }
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations",
      request,
    ).andIsOk
      .andAssertThatJson {
        node("languageTag").isEqualTo("de")
        node("text").isEqualTo("Neuer Begriff")
      }

    // Verify the translation was created by getting it
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations/de",
    ).andIsOk
      .andAssertThatJson {
        node("languageTag").isEqualTo("de")
        node("text").isEqualTo("Neuer Begriff")
      }
  }

  @Test
  fun `updates existing glossary term translation`() {
    // First create a translation
    val createRequest =
      UpdateGlossaryTermTranslationRequest().apply {
        languageTag = "cs"
        text = "Pojem"
      }
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations",
      createRequest,
    ).andIsOk

    // Then update it
    val updateRequest =
      UpdateGlossaryTermTranslationRequest().apply {
        languageTag = "cs"
        text = "Aktualizovaný pojem"
      }
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations",
      updateRequest,
    ).andIsOk
      .andAssertThatJson {
        node("languageTag").isEqualTo("cs")
        node("text").isEqualTo("Aktualizovaný pojem")
      }

    // Verify the translation was updated
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations/cs",
    ).andIsOk
      .andAssertThatJson {
        node("languageTag").isEqualTo("cs")
        node("text").isEqualTo("Aktualizovaný pojem")
      }
  }

  @Test
  fun `does not update glossary term translation when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request =
      UpdateGlossaryTermTranslationRequest().apply {
        languageTag = "de"
        text = "Neuer Begriff"
      }
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations",
      request,
    ).andIsBadRequest
  }

  @Test
  fun `gets existing glossary term translation`() {
    // Get the existing English translation from test data
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations/en",
    ).andIsOk
      .andAssertThatJson {
        node("languageTag").isEqualTo("en")
        node("text").isEqualTo("Term")
      }
  }

  @Test
  fun `gets default value for non-existent glossary term translation`() {
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations/it",
    ).andIsOk
      .andAssertThatJson {
        node("languageTag").isEqualTo("it")
        node("text").isEqualTo("")
      }
  }

  @Test
  fun `does not get glossary term translation when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}/translations/en",
    ).andIsBadRequest
  }
}
