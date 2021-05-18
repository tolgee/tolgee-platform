/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.testDataBuilder.data.ImportTestData
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.testng.annotations.Test

@SpringBootTest
class LanguageServiceTest : AbstractSpringTest() {
    @Test
    @Transactional
    fun `remove of language removes existing language reference from import language`() {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.root)

        var foundImportLanguage = importService.findLanguages(testData.import).first()
        assertThat(foundImportLanguage.existingLanguage!!.id).isEqualTo(testData.english.id)
        languageService.deleteLanguage(testData.english.id)
        entityManager.flush()
        entityManager.clear()
        foundImportLanguage = importService.findLanguages(testData.import).first()
        assertThat(foundImportLanguage.existingLanguage).isEqualTo(null)
    }
}
