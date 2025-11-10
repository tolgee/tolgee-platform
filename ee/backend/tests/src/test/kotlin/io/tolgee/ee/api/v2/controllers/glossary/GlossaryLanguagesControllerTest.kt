package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class GlossaryLanguagesControllerTest : AuthorizedControllerTest() {
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
  fun `gets all languages in use by the glossary`() {
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/languages")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.glossaryLanguageDtoList").isArray.hasSize(2).contains(
          json("{\"tag\": \"en\", \"base\": true}"),
          json("{\"tag\": \"cs\", \"base\": false}"),
        )
      }
  }

  @Test
  fun `gets empty response for glossary with no translations`() {
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.emptyGlossary.id}/languages")
      .andIsOk
      .andAssertThatJson {
        isObject.isEmpty()
      }
  }

  @Test
  fun `does not get languages when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/languages")
      .andIsBadRequest
  }
}
