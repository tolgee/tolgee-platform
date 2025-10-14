package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@Transactional
class TranslationServiceTest : AbstractSpringTest() {
  @Transactional
  @Test
  fun getTranslations() {
    val id = dbPopulator.populate().project.id
    val data =
      translationService.getTranslations(
        languageTags = HashSet(listOf("en", "de")),
        namespace = null,
        branch = null,
        projectId = id,
        structureDelimiter = '.',
      )
    assertThat(data["en"]).isInstanceOf(MutableMap::class.java)
  }

  @Transactional
  @Test
  fun `returns correct map when collision`() {
    val project = dbPopulator.populate().project
    keyService.create(project, CreateKeyDto("folder.folder", null, mapOf("en" to "Ha")))
    keyService.create(project, CreateKeyDto("folder.folder.translation", null, mapOf("en" to "Ha")))

    val viewData =
      translationService.getTranslations(
        languageTags = HashSet(listOf("en", "de")),
        namespace = null,
        branch = null,
        projectId = project.id,
        structureDelimiter = '.',
      )
    @Suppress("UNCHECKED_CAST")
    assertThat(viewData["en"] as Map<String, *>).containsKey("folder.folder.translation")
  }

  @Test
  fun `adds stats on translation save and update`() {
    val testData =
      executeInNewTransaction {
        val testData = TranslationsTestData()
        testDataService.saveTestData(testData.root)
        val translation = testData.aKeyGermanTranslation
        assertThat(translation.wordCount).isEqualTo(2)
        assertThat(translation.characterCount).isEqualTo(translation.text!!.length)
        testData
      }
    executeInNewTransaction {
      val translation = translationService.get(testData.aKeyGermanTranslation.id)
      translation.text = "My dog is cool!"
      translationService.save(translation)
    }

    executeInNewTransaction {
      val updated = translationService.get(testData.aKeyGermanTranslation.id)
      assertThat(updated.wordCount).isEqualTo(4)
      assertThat(updated.characterCount).isEqualTo(updated.text!!.length)
    }
  }

  @Transactional
  @Test
  fun `clears auto translation when set empty`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
    translationService.setForKey(testData.aKey, mapOf("de" to ""))
    val translation = translationService.get(testData.aKeyGermanTranslation.id)
    assertThat(translation.auto).isFalse
    assertThat(translation.mtProvider).isNull()
  }

  @Test
  fun `returns translations for default branch`() {
    val testData = TranslationsTestData()
    testData.generateBranchedData(10)
    testDataService.saveTestData(testData.root)
    val translations = translationService.getTranslations(
      languageTags = HashSet(listOf("en", "de")),
      namespace = null,
      branch = null,
      projectId = testData.project.id,
      structureDelimiter = '.',
    )
    assertThat(translations).hasSize(2)
    assertThat(translations["en"] as LinkedHashMap<*, *>).hasSize(1)
  }

  @Test
  @Transactional
  fun `returns translations for feature branch`() {
    val testData = TranslationsTestData()
    testData.generateBranchedData(10)
    testDataService.saveTestData(testData.root)
    val translations = translationService.getTranslations(
      languageTags = HashSet(listOf("en", "de")),
      namespace = null,
      branch = "feature-branch",
      projectId = testData.project.id,
      structureDelimiter = '.',
    )
    assertThat(translations).hasSize(2)
    assertThat(translations["en"] as LinkedHashMap<*, *>).hasSize(10)
  }

  @Test
  @Transactional
  fun `returns empty translations for invalid branch name`() {
    val testData = TranslationsTestData()
    testData.generateBranchedData(5)
    testDataService.saveTestData(testData.root)
    val translations = translationService.getTranslations(
      languageTags = HashSet(listOf("en", "de")),
      namespace = null,
      branch = "invalid-branch",
      projectId = testData.project.id,
      structureDelimiter = '.',
    )
    assertThat(translations).hasSize(0)
  }
}
