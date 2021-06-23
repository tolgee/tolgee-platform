package io.tolgee.service.query_builders

import io.tolgee.AbstractSpringTest
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.model.Language
import io.tolgee.model.Project
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@SpringBootTest
class TranslationsViewBuilderTest : AbstractSpringTest() {

    lateinit var project: Project
    lateinit var englishLanguage: Language
    lateinit var germanLanguage: Language

    @BeforeMethod
    fun setup() {
        testDataService.saveTestData {
            val user = addUserAccount {
                self {
                    username = "franta"
                }
            }.self
            project = addProject {
                self {
                    name = "Franta's project"
                    userOwner = user
                }
                englishLanguage = addLanguage {
                    self {
                        name = "English"
                        tag = "en"
                        originalName = "English"
                    }
                }.self
                germanLanguage = addLanguage {
                    self {
                        name = "German"
                        tag = "de"
                        originalName = "Deutsch"
                    }
                }.self

                addKey {
                    self.name = "A key"
                    addTranslation {
                        self {
                            key = this@addKey.self
                            language = germanLanguage
                            text = "Z translation"
                        }
                    }
                }

                addKey {
                    self.name = "Z key"
                    addTranslation {
                        self {
                            key = this@addKey.self
                            language = englishLanguage
                            text = "A translation"
                        }
                    }
                }

                (0..100).forEach {
                    addKey {
                        self { name = "key $it" }
                        addTranslation {
                            self {
                                key = this@addKey.self
                                language = germanLanguage
                                text = "I am key $it's german translation."
                            }
                        }
                        addTranslation {
                            self {
                                key = this@addKey.self
                                language = englishLanguage
                                text = "I am key $it's english translation."
                            }
                        }
                    }
                }
            }.self
        }
    }


    @Test
    fun `returns correct page size and page meta`() {
        val result = V2TranslationsViewBuilder.getData(
                em = entityManager,
                project = project,
                languages = setOf(englishLanguage, germanLanguage),
                searchString = null,
                PageRequest.of(0, 10))
        assertThat(result.content).hasSize(10)
        assertThat(result.totalElements).isGreaterThan(101)
    }

    @Test
    fun `sorts data correctly by de text`() {
        val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("translations.de.text")))

        val result = V2TranslationsViewBuilder.getData(
                em = entityManager,
                project = project,
                languages = setOf(englishLanguage, germanLanguage),
                searchString = null,
                pageRequest)
        assertThat(result.content.first().translations["de"]?.text).isEqualTo("Z translation")
    }

    @Test
    fun `sorts data correctly by en text`() {
        val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("translations.en.text")))

        val result = V2TranslationsViewBuilder.getData(
                em = entityManager,
                project = project,
                languages = setOf(englishLanguage, germanLanguage),
                searchString = null,
                pageRequest)
        assertThat(result.content.first().translations["en"]?.text).isEqualTo("A translation")
    }
}
