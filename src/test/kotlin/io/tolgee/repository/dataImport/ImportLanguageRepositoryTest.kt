package io.tolgee.repository.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.TestDataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.Test

@SpringBootTest
class ImportLanguageRepositoryTest : AbstractSpringTest() {

    @Autowired
    lateinit var importRepository: ImportRepository

    @Autowired
    lateinit var testDataService: TestDataService

    @Test
    fun `creates, saves and gets Import entity`() {

    }
}
