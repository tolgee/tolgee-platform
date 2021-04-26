package io.tolgee.repository

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.ImportTestData
import io.tolgee.service.dataImport.ImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.springframework.transaction.annotation.Transactional
import org.testng.annotations.Test
import javax.persistence.EntityManager

@SpringBootTest
@Transactional
class LanguageRepositoryTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    lateinit var importService: ImportService

    @Autowired
    lateinit var languageRepository: LanguageRepository

    @Autowired
    lateinit var testDataService: TestDataService

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun `remove of language removes existing language reference from import language`() {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.data)

        var foundImportLanguage = importService.findLanguages(testData.import).first()
        assertThat(foundImportLanguage.existingLanguage).isEqualTo(testData.english)
        languageRepository.delete(testData.english)
        languageRepository.flush()
        entityManager.clear()
        foundImportLanguage = importService.findLanguages(testData.import).first()
        assertThat(foundImportLanguage.existingLanguage).isEqualTo(null)
    }
}

