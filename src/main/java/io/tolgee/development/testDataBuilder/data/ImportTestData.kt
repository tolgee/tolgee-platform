package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.DataBuilders
import io.tolgee.development.testDataBuilder.TestDataBuilder
import io.tolgee.model.*
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation

class ImportTestData {
    lateinit var conflict: Translation
    lateinit var importBuilder: DataBuilders.ImportBuilder
    lateinit var import: Import
    lateinit var english: Language
    lateinit var german: Language
    lateinit var czech: Language
    lateinit var french: Language
    lateinit var importFrench: ImportLanguage
    lateinit var importEnglish: ImportLanguage
    lateinit var translationWithConflict: ImportTranslation
    lateinit var repository: Repository
    lateinit var userAccount: UserAccount

    val root: TestDataBuilder = TestDataBuilder().apply {
        userAccount = addUserAccount {
            self { username = "franta" }
        }.self
        repository = addRepository {
            self { name = "test" }
            addPermission {
                self {
                    repository = this@addRepository.self
                    user = this@ImportTestData.userAccount
                    type = Permission.RepositoryPermissionType.MANAGE
                }
            }

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
            conflict = addTranslation {
                self {
                    this.language = english
                    this.key = key
                    this.text = "What a text"
                }
            }.self
            addTranslation {
                self {
                    this.language = english
                    this.key = repositoryBuilder.data.keys[1].self
                    this.text = "What a text"
                }
            }.self
            addTranslation {
                self {
                    this.language = english
                    this.key = repositoryBuilder.data.keys[2].self
                    this.text = "What a text"
                }
            }.self
            addTranslation {
                self {
                    this.language = english
                    this.key = repositoryBuilder.data.keys[3].self
                    this.text = "What a text"
                }
            }.self
            addTranslation {
                self {
                    this.language = french
                    this.key = repositoryBuilder.data.keys[0].self
                    this.text = "What a french text"
                }
            }.self
            addTranslation {
                self {
                    this.language = french
                    this.key = repositoryBuilder.data.keys[1].self
                    this.text = "What a french text 2"
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
                    importFrench = addImportLanguage {
                        self.name = "fr"
                    }.self
                    addImportLanguage {
                        self.name = "de"
                        self.existingLanguage = german
                    }.self

                    val addedKey = addImportKey {
                        self {
                            name = "what a key"
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

                    translationWithConflict = addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = addedKey.self
                            this.conflict = this@ImportTestData.conflict
                            this.text = "Overridden"
                        }
                    }.self
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[1].self
                            this.conflict = repositoryBuilder.data.translations[1].self
                            this.text = "Imported text"
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[2].self
                            this.conflict = repositoryBuilder.data.translations[2].self
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[3].self
                            this.conflict = repositoryBuilder.data.translations[3].self

                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[4].self
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = this@addImport.data.importFiles[0].data.importKeys[5].self
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importFrench
                            this.key = this@addImport.data.importFiles[0].data.importKeys[0].self
                            this.text = "French text"
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importFrench
                            this.key = this@addImport.data.importFiles[0].data.importKeys[2].self
                            this.text = "French text"
                        }
                    }
                }
            }
            import = importBuilder.self
        }.self
    }

    operator fun invoke(ft: DataBuilders.ImportBuilder.() -> Unit): TestDataBuilder {
        ft(importBuilder)
        return root
    }

    fun setAllResolved() {
        this.importBuilder.data.importFiles.forEach { file ->
            file.data.importTranslations.forEach {
                it.self { resolved = true }
            }
        }
    }

    fun setAllOverride() {
        this.importBuilder.data.importFiles.forEach { file ->
            file.data.importTranslations.forEach {
                it.self { override = true }
            }
        }
    }
}
