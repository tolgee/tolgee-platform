package io.tolgee.ee.api.v2.controllers.glossary

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andIsOk
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
class GlossaryExportControllerTest : AuthorizedControllerTest() {
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
  fun `exports all associated languages including empty ones`() {
    // The glossary has base language "en"
    // The project has languages: en, fr, cs
    // Only terms have translations in "en" and "cs", but not "fr"
    // The export should include all three languages: en (term column), cs, fr

    val result =
      performAuthGet("/v2/organizations/${testData.organization.id}/glossaries/${testData.glossary.id}/export")
        .andIsOk
        .andReturn()

    val csvContent = result.response.contentAsString
    val lines = csvContent.lines()

    // Check the header line
    val headerLine = lines[0]
    assertThat(headerLine).isEqualToNormalizingNewlines(
      """
      "term","description","translatable","casesensitive","abbreviation","forbidden","cs","fr"
      """.trimIndent(),
    )
  }
}
