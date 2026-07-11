package io.tolgee.ee.service.glossary

import io.tolgee.component.machineTranslation.metadata.TranslationGlossaryItem
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryAiTranslationTestData
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class GlossaryTermsForAiTranslationTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var glossaryTermService: GlossaryTermService

  lateinit var testData: GlossaryAiTranslationTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.GLOSSARY)
    testData = GlossaryAiTranslationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.userOwner
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private fun getTerms(): Map<String, TranslationGlossaryItem> {
    val projectDto: ProjectDto = projectService.getDto(testData.project.id)
    return glossaryTermService
      .getGlossaryTerms(projectDto, "en", "fr", testData.sourceText)
      .associateBy { it.source }
  }

  @Test
  fun `includes a term that has a translation in the target language`() {
    val term = getTerms()["Apple"]
    assertThat(term).isNotNull
    assertThat(term!!.target).isEqualTo("Pomme")
  }

  @Test
  fun `excludes an untranslated term with no description and no flags`() {
    assertThat(getTerms()).doesNotContainKey("Banana")
  }

  @Test
  fun `includes an untranslated term that has a description`() {
    val term = getTerms()["Cherry"]
    assertThat(term).isNotNull
    assertThat(term!!.target).isNull()
    assertThat(term.description).isEqualTo("Translate as the fruit, never as a name")
  }

  @Test
  fun `includes an untranslated non-translatable term`() {
    val term = getTerms()["Dragon"]
    assertThat(term).isNotNull
    assertThat(term!!.target).isNull()
    assertThat(term.isNonTranslatable).isTrue()
  }

  @Test
  fun `includes an untranslated forbidden term`() {
    val term = getTerms()["Elder"]
    assertThat(term).isNotNull
    assertThat(term!!.target).isNull()
    assertThat(term.isForbiddenTerm).isTrue()
  }

  @Test
  fun `includes an untranslated case-sensitive-only term`() {
    val term = getTerms()["Fig"]
    assertThat(term).isNotNull
    assertThat(term!!.target).isNull()
    assertThat(term.isCaseSensitive).isTrue()
  }

  @Test
  fun `includes an untranslated abbreviation-only term`() {
    val term = getTerms()["Grape"]
    assertThat(term).isNotNull
    assertThat(term!!.target).isNull()
    assertThat(term.isAbbreviation).isTrue()
  }
}
