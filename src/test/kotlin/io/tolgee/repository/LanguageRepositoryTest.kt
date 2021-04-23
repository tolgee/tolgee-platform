package io.tolgee.repository

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.model.Language
import io.tolgee.model.dataImport.Import
import io.tolgee.service.dataImport.ImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.annotations.Test
import javax.persistence.EntityManager

@SpringBootTest
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
    fun `remove of language removes existing language reference from import language`() {
        var language: Language? = null
        var import: Import? = null

        testDataService.buildTestData {
            addUserAccount {
                name = "franta"
            }
            addRepository {
                self { name = "test" }
                language = addLanguage {
                    name = "English"
                    abbreviation = "en"
                }
                import = addImport {
                    addFile {
                        addLanguage {
                            self.name = "en"
                            self.existingLanguage = language
                        }.self
                        val addedKey = addKey {
                            self {
                                name = "cool_key"
                            }
                        }
                        addTranslation {
                            self {
                                key = addedKey.self
                            }
                        }
                    }
                }.self
            }
        }

        var foundImportLanguage = importService.findLanguages(import!!).first()
        assertThat(foundImportLanguage.existingLanguage).isEqualTo(language)
        languageRepository.delete(language!!)
        languageRepository.flush()
        entityManager.clear()
        foundImportLanguage = importService.findLanguages(import!!).first()
        assertThat(foundImportLanguage.existingLanguage).isEqualTo(null)
    }
}

