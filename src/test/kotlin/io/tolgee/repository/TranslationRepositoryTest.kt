package io.tolgee.repository

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.model.Translation
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.service.dataImport.ImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.annotations.Test
import javax.persistence.EntityManager

@SpringBootTest
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
    fun `remove of language removes existing language reference from import language`() {
        var import: Import? = null
        var importLanguage: ImportLanguage? = null
        var collision: Translation? = null

        testDataService.buildTestData {
            addUserAccount {
                username = "franta"
            }
            addRepository {
                self { name = "test" }
                val key = addKey {
                    self { name = "what a key" }
                }.self
                collision = addTranslation {
                    self {
                        this.key = key
                    }
                }.self
                import = addImport {
                    addImportFile {
                        importLanguage = addImportLanguage {
                            self.name = "en"
                        }.self
                        val addedKey = addImportKey {
                            self {
                                name = "cool_key"
                            }
                        }
                        addImportTranslation {
                            self {
                                this.key = addedKey.self
                                this.collision = collision
                            }
                        }
                    }
                }.self
            }
        }

        assertThat(importService.findTranslations(import!!, importLanguage!!.id).first().collision).isEqualTo(collision)
        translationRepository.delete(collision!!)
        entityManager.clear()
        assertThat(importService.findTranslations(import!!, importLanguage!!.id).first().collision).isEqualTo(null)
    }
}

