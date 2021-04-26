package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.DataBuilders
import io.tolgee.development.testDataBuilder.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Translation
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage

class ImportTestData {
    lateinit var collision: Translation
    lateinit var importBuilder: DataBuilders.ImportBuilder
    lateinit var import: Import
    lateinit var english: Language
    lateinit var german: Language
    lateinit var czech: Language
    lateinit var french: Language
    lateinit var importEnglish: ImportLanguage

    val data: TestDataBuilder = TestDataBuilder().apply {
        addUserAccount {
            username = "franta"
        }
        addRepository {
            self { name = "test" }
            val key = addKey {
                self { name = "what a key" }
            }.self
            addKey {
                self { name = "what a nice key" }
            }.self
            addKey {
                self { name = "what a beautiful key" }
            }.self
            addKey {
                self { name = "another nice key" }
            }.self
            addKey {
                self { name = "extraordinary key" }
            }.self
            addKey {
                self { name = "this is another key" }
            }.self
            english = addLanguage {
                self {
                    name = "English"
                    abbreviation = "en"
                }
            }.self
            german = addLanguage {
                self {
                    name = "German"
                    abbreviation = "de"
                }
            }.self
            czech = addLanguage {
                self {
                    name = "Czech"
                    abbreviation = "cs"
                }
            }.self
            french = addLanguage {
                self {
                    name = "French"
                    abbreviation = "fr"
                }
            }.self
            collision = addTranslation {
                self {
                    this.language = english
                    this.key = key
                }
            }.self
            addTranslation {
                self {
                    this.language = english
                    this.key = repositoryBuilder.data.keys[1].self
                }
            }.self
            addTranslation {
                self {
                    this.language = english
                    this.key = repositoryBuilder.data.keys[2].self
                }
            }.self
            addTranslation {
                self {
                    this.language = english
                    this.key = repositoryBuilder.data.keys[3].self
                }
            }.self
            importBuilder = addImport {
                addImportFile {
                    self {
                        name = "multilang.json"
                    }
                    importEnglish = addImportLanguage {
                        self.name = "en"
                        self.existingLanguage = english
                    }.self
                    addImportLanguage {
                        self.name = "fr"
                        self.existingLanguage = french
                    }.self
                    addImportLanguage {
                        self.name = "de"
                        self.existingLanguage = german
                    }.self

                    val addedKey = addImportKey {
                        self {
                            name = "cool_key"
                        }
                    }
                    addImportKey {
                        self { name = "what a nice key" }
                    }.self
                    addImportKey {
                        self { name = "what a beautiful key" }
                    }.self
                    addImportKey {
                        self { name = "another nice key" }
                    }.self
                    addImportKey {
                        self { name = "extraordinary key" }
                    }.self
                    addImportKey {
                        self { name = "this is another key" }
                    }.self

                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = addedKey.self
                            this.collision = this@ImportTestData.collision
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[1].self
                            this.collision = repositoryBuilder.data.translations[1].self
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[2].self
                            this.collision = repositoryBuilder.data.translations[2].self
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[3].self
                            this.collision = repositoryBuilder.data.translations[3].self
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[1].self
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[1].self
                        }
                    }
                }
            }
            import = importBuilder.self
        }
    }

    operator fun invoke(ft: DataBuilders.ImportBuilder.() -> Unit): TestDataBuilder {
        ft(importBuilder)
        return data
    }
}
