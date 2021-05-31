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
    var project: Project
    var userAccount: UserAccount

    val root: TestDataBuilder = TestDataBuilder().apply {
        userAccount = addUserAccount {
            self {
                username = "franta"
                name = "Frantisek Dobrota"
            }
        }.self
        project = addRepository {
            self { name = "test" }
            addPermission {
                self {
                    project = this@addRepository.self
                    user = this@ImportTestData.userAccount
                    type = Permission.ProjectPermissionType.MANAGE
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
                            this.key = data.importKeys[1].self
                            this.conflict = repositoryBuilder.data.translations[1].self
                            this.text = "Imported text"
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = data.importKeys[2].self
                            this.conflict = repositoryBuilder.data.translations[2].self
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = data.importKeys[3].self
                            this.conflict = repositoryBuilder.data.translations[3].self

                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = data.importKeys[4].self
                        }
                    }
                    addImportTranslation {
                        self {
                            this.language = importEnglish
                            this.key = data.importKeys[5].self
                        }
                    }
                }
            }
            import = importBuilder.self
        }.self
    }

    fun addFrenchTranslations() {
        this.importBuilder.data.importFiles[0].apply {
            addImportTranslation {
                self {
                    this.language = importFrench
                    this.key = data.importKeys[0].self
                    this.text = "French text"
                }
            }
            addImportTranslation {
                self {
                    this.language = importFrench
                    this.key = data.importKeys[2].self
                    this.text = "French text"
                }
            }
        }
    }

    operator fun invoke(ft: DataBuilders.ImportBuilder.() -> Unit): TestDataBuilder {
        ft(importBuilder)
        return root
    }

    fun setAllResolved() {
        this.importBuilder.data.importFiles.forEach { file ->
            file.data.importTranslations.forEach {
                it.self.resolve()
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

    fun addFileIssues() {
        this.importBuilder.data.importFiles[0].self {
            addKeyIsEmptyIssue(1)
            addKeyIsNotStringIssue(4, 2)
            addValueIsEmptyIssue("value_is_emtpy_key")
            addValueIsNotStringIssue("value_is_not_string_key", 5, 1)
        }
    }

    fun addManyFileIssues() {
        addFileIssues()
        this.importBuilder.data.importFiles[0].self {
            (1..200).forEach {
                addKeyIsEmptyIssue(it)
            }
        }
    }

    fun addManyTranslations() {
        addFileIssues()
        val repositoryBuilder = this.root.data.repositories[0]
        val import = repositoryBuilder.data.imports[0]
        import.addImportFile {
            self { name = "another.json" }
            val fr = addImportLanguage {
                self {
                    name = "fr"
                    existingLanguage = french
                }
            }.self
            (1..300).forEach { num ->
                repositoryBuilder.addKey {
                    val key = self {
                        name = "this_is_key_$num"
                    }
                    repositoryBuilder.addTranslation {
                        val translation = self {
                            this.key = key
                            this.language = english
                            this.text = "I am translation $num"
                        }
                        addImportKey {
                            self {
                                this.name = key.name!!
                            }
                            addImportTranslation {
                                self {
                                    language = fr
                                    this.key = this@addImportKey.self
                                    text = "I am import translation $num"
                                    conflict = translation
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    fun addKeyMetadata() {
        root.data.repositories[0].data.keys[2].addMeta {
            self {
                addComment(userAccount) {
                    text = "Hello I am first comment (I exist)"
                    fromImport = true
                }
                addCodeReference(userAccount) {
                    path = "./code/exist.extension"
                    line = 10
                    fromImport = true
                }
            }
        }

        root.data.repositories[0].data.imports[0].data.importFiles[0].data.importKeys[2].addMeta {
            self {
                addComment(userAccount) {
                    text = "Hello I am first comment (I exist)"
                    fromImport = true
                }
                addComment(userAccount) {
                    text = "Hello I am second comment (I dont exist)"
                    fromImport = true
                }
                addComment(userAccount) {
                    text = "One more"
                    fromImport = true
                }
                addCodeReference(userAccount) {
                    path = "./code/exist.extension"
                    line = 10
                    fromImport = true
                }
                addCodeReference(userAccount) {
                    path = "./code/notExist.extendison"
                    fromImport = false
                }
            }
        }

        root.data.repositories[0].data.imports[0].data.importFiles[0].data.importKeys[3].addMeta {
            self {
                addComment(userAccount) {
                    text = "Hello!"
                    fromImport = true
                }
                addCodeReference(userAccount) {
                    path = "./code/exist.extension"
                    line = 10
                    fromImport = true
                }

            }
        }
    }
}
