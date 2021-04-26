package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.DataBuilders
import io.tolgee.development.testDataBuilder.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Translation
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage

class ImportTestData {
    lateinit var collision: Translation
    lateinit var import: Import
    lateinit var english: Language
    lateinit var importEnglish: ImportLanguage

    fun base(ft: DataBuilders.ImportBuilder.() -> Unit): TestDataBuilder {
        return TestDataBuilder().apply {
            addUserAccount {
                username = "franta"
            }
            addRepository {
                self { name = "test" }
                val key = addKey {
                    self { name = "what a key" }
                }.self
                english = addLanguage {
                    self {
                        name = "English"
                        abbreviation = "en"
                    }
                }.self
                collision = addTranslation {
                    self {
                        this.key = key
                    }
                }.self
                import = addImport {
                    addImportFile {
                        importEnglish = addImportLanguage {
                            self.name = "en"
                            self.existingLanguage = english
                        }.self
                        val addedKey = addImportKey {
                            self {
                                name = "cool_key"
                            }
                        }
                        addImportTranslation {
                            self {
                                this.key = addedKey.self
                                this.collision = this@ImportTestData.collision
                            }
                        }
                    }
                    ft(this)
                }.self
            }
        }
    }
}
