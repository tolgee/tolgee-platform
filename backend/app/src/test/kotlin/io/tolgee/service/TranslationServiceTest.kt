package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@Transactional
class TranslationServiceTest : AbstractSpringTest() {
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
    translationService.set(testData.aKey, mapOf("de" to ""), testData.project.id)
    val translation = translationService.get(testData.aKeyGermanTranslation.id)
    assertThat(translation.auto).isFalse
    assertThat(translation.mtProvider).isNull()
  }
}
