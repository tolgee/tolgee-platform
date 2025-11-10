package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.glossary.CreateGlossaryTermWithTranslationRequest
import io.tolgee.ee.data.glossary.DeleteMultipleGlossaryTermsRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryTermWithTranslationRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
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
class GlossaryTermControllerTest : AuthorizedControllerTest() {
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
  fun `returns all glossary terms`() {
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms",
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.glossaryTerms").isArray.hasSize(4)
        node("_embedded.glossaryTerms[0].id").isValidId
        inPath("_embedded.glossaryTerms[*].description").isArray.containsExactlyInAnyOrder(
          "Forbidden term",
          "The description",
          "The multiword term",
          "Trademark",
        )
      }
  }

  @Test
  fun `does not return all glossary terms when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms")
      .andIsBadRequest
  }

  @Test
  fun `returns all glossary terms with translations`() {
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/termsWithTranslations?sort=description,asc",
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.glossaryTerms") {
          isArray.hasSize(4)
          node("[0].id").isValidId
          node("[0].description").isEqualTo("Forbidden term")
          node("[0].translations") {
            isArray.hasSize(2)
          }
          node("[1].description").isEqualTo("The description")
          node("[1].translations") {
            isArray.hasSize(1)
            node("[0].text").isEqualTo("Term")
          }
          node("[2].description").isEqualTo("The multiword term")
          node("[2].translations") {
            isArray.hasSize(2)
          }
          node("[3].description").isEqualTo("Trademark")
          node("[3].translations") {
            isArray.hasSize(1)
            node("[0].text").isEqualTo("Apple")
          }
        }
      }
  }

  @Test
  fun `does not return all glossary terms with translations when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/termsWithTranslations",
    ).andIsBadRequest
  }

  @Test
  fun `returns all glossary term ids`() {
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/termsIds")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.longList") {
          isArray.hasSize(4)
        }
      }
  }

  @Test
  fun `does not return all glossary term ids when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/termsIds")
      .andIsBadRequest
  }

  @Test
  fun `returns single glossary term`() {
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("description").isEqualTo("The description")
        node("flagNonTranslatable").isEqualTo(false)
        node("flagCaseSensitive").isEqualTo(false)
        node("flagAbbreviation").isEqualTo(false)
        node("flagForbiddenTerm").isEqualTo(false)
      }
  }

  @Test
  fun `does not return single glossary term when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsBadRequest
  }

  @Test
  fun `creates glossary term`() {
    val request =
      CreateGlossaryTermWithTranslationRequest().apply {
        description = "New Term Description"
        flagNonTranslatable = true
        flagCaseSensitive = true
        text = "New Term"
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsOk
      .andAssertThatJson {
        node("term.id").isValidId.satisfies({
          performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/$it")
            .andIsOk
            .andAssertThatJson {
              node("id").isValidId.isEqualTo(it)
              node("description").isEqualTo("New Term Description")
              node("flagNonTranslatable").isEqualTo(true)
              node("flagCaseSensitive").isEqualTo(true)
            }
        })
        node("term.description").isEqualTo("New Term Description")
        node("term.flagNonTranslatable").isEqualTo(true)
        node("term.flagCaseSensitive").isEqualTo(true)
        node("translation.text").isEqualTo("New Term")
      }
  }

  @Test
  fun `does not create glossary term when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request =
      CreateGlossaryTermWithTranslationRequest().apply {
        description = "New Term Description"
        text = "New Term"
      }
    performAuthPost("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsBadRequest
  }

  @Test
  fun `updates glossary term`() {
    val request =
      UpdateGlossaryTermWithTranslationRequest().apply {
        description = "Updated Description"
        flagNonTranslatable = true
        flagCaseSensitive = true
        text = "Updated Term"
      }
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
      request,
    ).andIsOk
      .andAssertThatJson {
        node("term.id").isValidId
        node("term.description").isEqualTo("Updated Description")
        node("term.flagNonTranslatable").isEqualTo(true)
        node("term.flagCaseSensitive").isEqualTo(true)
        node("translation.text").isEqualTo("Updated Term")
      }

    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("description").isEqualTo("Updated Description")
        node("flagNonTranslatable").isEqualTo(true)
        node("flagCaseSensitive").isEqualTo(true)
      }
  }

  @Test
  fun `does not update glossary term when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request =
      UpdateGlossaryTermWithTranslationRequest().apply {
        description = "Updated Description"
        text = "Updated Term"
      }
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
      request,
    ).andIsBadRequest
  }

  @Test
  fun `deletes glossary term`() {
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsOk

    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsNotFound
  }

  @Test
  fun `does not delete glossary term when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsBadRequest
  }

  @Test
  fun `deletes multiple glossary terms`() {
    val request =
      DeleteMultipleGlossaryTermsRequest().apply {
        termIds = setOf(testData.term.id, testData.trademarkTerm.id)
      }
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsOk

    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.term.id}",
    ).andIsNotFound

    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.trademarkTerm.id}",
    ).andIsNotFound

    // Verify the third term still exists
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms/${testData.forbiddenTerm.id}",
    ).andIsOk
  }

  @Test
  fun `does not delete multiple glossary terms when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request =
      DeleteMultipleGlossaryTermsRequest().apply {
        termIds = setOf(testData.term.id, testData.trademarkTerm.id)
      }
    performAuthDelete("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/terms", request)
      .andIsBadRequest
  }
}
