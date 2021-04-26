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
class TranslationRepositoryTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    lateinit var importService: ImportService

    @Autowired
    lateinit var translationRepository: TranslationRepository

    @Autowired
    lateinit var testDataService: TestDataService

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun `remove of language removes existing language reference from import language`() {
        val testData = ImportTestData()

        testDataService.saveTestData(testData.base { })

        assertThat(importService.findTranslations(testData.import, testData.importEnglish.id).first().collision)
                .isEqualTo(testData.collision)
        translationRepository.delete(testData.collision)
        entityManager.flush()
        entityManager.clear()
        assertThat(importService.findTranslations(testData.import, testData.importEnglish.id).first().collision)
                .isEqualTo(null)
    }
}

