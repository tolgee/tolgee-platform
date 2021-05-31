package io.tolgee.repository

import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.service.LanguageService
import io.tolgee.service.dataImport.ImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@SpringBootTest
@Transactional
class LanguageRepositoryTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    lateinit var importService: ImportService

    @Autowired
    lateinit var languageService: LanguageService

    @Autowired
    lateinit var testDataService: TestDataService

    @Autowired
    lateinit var entityManager: EntityManager
}

