package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ImportBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.translation.Translation

class ImportTestData {
  lateinit var conflict: Translation
  lateinit var importBuilder: ImportBuilder
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
      username = "franta"
      name = "Frantisek Dobrota"
    }.self
    project = addProject { name = "test" }.build project@{
      addPermission {
        project = this@project.self
        user = this@ImportTestData.userAccount
        type = Permission.ProjectPermissionType.MANAGE
      }

      val key = addKey {
        name = "what a key"
      }.self
      addKey {
        name = "what a nice key"
      }.self
      addKey {
        name = "what a beautiful key"
      }.self
      addKey {
        name = "another nice key"
      }.self
      addKey {
        name = "extraordinary key"
      }.self
      addKey {
        name = "this is another key"
      }.self
      english = addLanguage {
        name = "English"
        tag = "en"
      }.self
      german = addLanguage {
        name = "German"
        tag = "de"
      }.self
      czech = addLanguage {
        name = "Czech"
        tag = "cs"
      }.self
      french = addLanguage {
        name = "French"
        tag = "fr"
      }.self
      conflict = addTranslation {
        this.language = english
        this.key = key
      }.self
      addTranslation {
        this.language = english
        this.key = this@project.data.keys[1].self
      }.self
      addTranslation {
        this.language = english
        this.key = this@project.data.keys[2].self
      }.self
      addTranslation {
        this.language = english
        this.key = this@project.data.keys[3].self
      }.self
      addTranslation {
        this.language = french
        this.key = this@project.data.keys[0].self
        this.text = "What a french text"
      }.self
      addTranslation {
        this.language = french
        this.key = this@project.data.keys[1].self
        this.text = "What a french text 2"
      }.self
      importBuilder = addImport {}.build {
        addImportFile {
          name = "multilang.json"
        }.build {
          importEnglish = addImportLanguage {
            name = "en"
            existingLanguage = english
          }.self
          importFrench = addImportLanguage {
            name = "fr"
          }.self
          addImportLanguage {
            name = "de"
            existingLanguage = german
          }

          val addedKey = addImportKey {
            name = "what a key"
          }
          addImportKey {
            name = "what a nice key"
          }
          addImportKey {
            name = "what a beautiful key"
          }
          addImportKey {
            name = (1..2000).joinToString("") { "a" }
          }
          addImportKey {
            name = "extraordinary key"
          }
          addImportKey {
            name = "this is another key"
          }

          translationWithConflict = addImportTranslation {
            this.language = importEnglish
            this.key = addedKey.self
            this.conflict = this@ImportTestData.conflict
            this.text = "Overridden"
          }.self
          addImportTranslation {
            this.language = importEnglish
            this.key = data.importKeys[1].self
            this.conflict = projectBuilder.data.translations[1].self
            this.text = "Imported text"
          }
          addImportTranslation {
            this.language = importEnglish
            this.key = data.importKeys[2].self
            this.conflict = projectBuilder.data.translations[2].self
          }
          addImportTranslation {
            this.language = importEnglish
            this.key = data.importKeys[3].self
            this.conflict = projectBuilder.data.translations[3].self
          }
          addImportTranslation {
            this.language = importEnglish
            this.key = data.importKeys[4].self
          }
          addImportTranslation {
            this.language = importEnglish
            this.key = data.importKeys[5].self
          }
        }
      }
      import = importBuilder.self
    }.self
  }

  fun addFrenchTranslations() {
    this.importBuilder.data.importFiles[0].apply {
      addImportTranslation {
        this.language = importFrench
        this.key = data.importKeys[0].self
        this.text = "French text"
      }
      addImportTranslation {
        this.language = importFrench
        this.key = data.importKeys[2].self
        this.text = "French text"
      }
    }
  }

  operator fun invoke(ft: ImportBuilder.() -> Unit): TestDataBuilder {
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
    val projectBuilder = this.root.data.projects[0]
    val import = projectBuilder.data.imports[0]
    import.addImportFile {
      name = "another.json"
    }.build {
      val fr = addImportLanguage {
        name = "fr"
        existingLanguage = french
      }.self
      (1..300).forEach { num ->
        projectBuilder.addKey {
          name = "this_is_key_$num"
        }.build keyBuilder@{
          projectBuilder.addTranslation {
            this.key = this@keyBuilder.self
            this.language = english
            this.text = "I am translation $num"
          }.build buildTranslation@{
            addImportKey {
              name = this@keyBuilder.self.name
              addImportTranslation {
                language = fr
                this.key = this@addImportKey
                text = "I am import translation $num"
                conflict = this@buildTranslation.self
              }
            }
          }
        }
      }
    }
  }

  fun addKeyMetadata() {
    root.data.projects[0].data.keys[2].addMeta {
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

    root.data.projects[0].data.imports[0].data.importFiles[0].data.importKeys[2].addMeta {
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

    root.data.projects[0].data.imports[0].data.importFiles[0].data.importKeys[3].addMeta {
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
