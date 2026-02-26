package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.builders.ImportBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
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
  lateinit var newLongKey: ImportKey
  lateinit var featureBranch: Branch
  lateinit var defaultBranch: Branch
  var project: Project
  var userAccount: UserAccount
  val projectBuilder get() = root.data.projects[0]

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccount =
        addUserAccount {
          username = "franta"
          name = "Frantisek Dobrota"
        }.self
      project =
        addProject { name = "test" }
          .build project@{
            addPermission {
              project = this@project.self
              user = this@ImportTestData.userAccount
              type = ProjectPermissionType.MANAGE
            }

            val key =
              addKey {
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
            english =
              addLanguage {
                name = "English"
                tag = "en"
              }.self
            german =
              addLanguage {
                name = "German"
                tag = "de"
              }.self
            czech =
              addLanguage {
                name = "Czech"
                tag = "cs"
              }.self
            french =
              addLanguage {
                name = "French"
                tag = "fr"
              }.self
            conflict =
              addTranslation {
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
              this.auto = true
              this.mtProvider = MtServiceType.GOOGLE
              this.language = french
              this.key = this@project.data.keys[0].self
              this.text = "What a french text"
            }.self
            addTranslation {
              this.auto = true
              this.mtProvider = MtServiceType.GOOGLE
              this.language = french
              this.key = this@project.data.keys[1].self
              this.text = "What a french text 2"
            }.self
            importBuilder =
              addImport {
                author = userAccount
              }.build {
                addImportFile {
                  name = "multilang.json"
                }.build {
                  importEnglish =
                    addImportLanguage {
                      name = "en"
                      existingLanguage = english
                    }.self
                  importFrench =
                    addImportLanguage {
                      name = "fr"
                    }.self
                  addImportLanguage {
                    name = "de"
                    existingLanguage = german
                  }

                  val addedKey =
                    addImportKey {
                      name = "what a key"
                    }.build {
                      addMeta {
                        description = "This is a key"
                      }
                    }
                  addImportKey {
                    name = "what a nice key"
                  }
                  addImportKey {
                    name = "what a beautiful key"
                  }
                  addImportKey {
                    name = (1..2000).joinToString("") { "a" }
                    newLongKey = this
                  }
                  addImportKey {
                    name = "extraordinary key"
                  }
                  addImportKey {
                    name = "this is another key"
                  }

                  translationWithConflict =
                    addImportTranslation {
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

  fun useTranslateOnlyUser(): UserAccount {
    val user =
      this.root.addUserAccount {
        name = "En only user"
        username = "en_only_user"
      }
    this.import.author = user.self
    this.projectBuilder.addPermission {
      this.user = user.self
      this.scopes = arrayOf(Scope.TRANSLATIONS_EDIT)
      this.type = null
      viewLanguages.add(english)
      translateLanguages.add(english)
    }
    return user.self
  }

  fun addKeyWithTag(keyTag: String): io.tolgee.model.key.Key {
    return this.projectBuilder
      .addKey {
        name = "key with tag"
        this.keyMeta
      }.build {
        addTag(keyTag)
      }.self
  }

  fun addDefaultBranch(): Branch {
    defaultBranch =
      projectBuilder
        .addBranch {
          name = "main"
          isDefault = true
          isProtected = true
        }.self
    return defaultBranch
  }

  fun useViewEnOnlyUser(): UserAccount {
    val user =
      this.root.addUserAccount {
        name = "En only user"
        username = "en_only_user"
      }
    this.import.author = user.self
    this.projectBuilder.addPermission {
      this.user = user.self
      this.scopes = arrayOf(Scope.TRANSLATIONS_VIEW)
      this.type = null
      viewLanguages.add(english)
    }
    return user.self
  }

  fun addEmptyKey() {
    this.importBuilder.data.importFiles[0].apply {
      addImportKey {
        name = "empty key"
      }
    }
  }

  fun addFrenchTranslations(): () -> List<Unit> {
    val translations = mutableListOf<ImportTranslation>()
    this.importBuilder.data.importFiles[0].apply {
      addImportTranslation {
        this.language = importFrench
        this.key = data.importKeys[0].self
        this.text = "French text"
        translations.add(this)
      }
      addImportTranslation {
        this.language = importFrench
        this.key = data.importKeys[2].self
        this.text = "French text"
        translations.add(this)
      }
    }

    return {
      this.importFrench.existingLanguage = this.french
      translations.map { it.resolve() }
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
    import
      .addImportFile {
        name = "another.json"
      }.build {
        val fr =
          addImportLanguage {
            name = "fr"
            existingLanguage = french
          }.self
        (1..300).forEach { num ->
          projectBuilder
            .addKey {
              name = "this_is_key_$num"
            }.build keyBuilder@{
              projectBuilder
                .addTranslation {
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
    projectBuilder.data.keys[2].addMeta {
      addComment(userAccount) {
        text = "Hello I am first comment (I exist)"
        fromImport = true
      }
      addCodeReference(userAccount) {
        path = "./code/exist.extension"
        line = 10
        fromImport = true
        author = userAccount
      }
    }

    projectBuilder.data.imports[0].data.importFiles[0].data.importKeys[2].addMeta {
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
        author = userAccount
      }
      addCodeReference(userAccount) {
        path = "./code/notExist.extendison"
        fromImport = false
        author = userAccount
      }
    }

    projectBuilder.data.imports[0].data.importFiles[0].data.importKeys[3].addMeta {
      addComment(userAccount) {
        text = "Hello!"
        fromImport = true
      }
      addCodeReference(userAccount) {
        path = "./code/exist.extension"
        line = 10
        fromImport = true
        author = userAccount
      }
    }
  }

  fun addFilesWithNamespaces(): AddFilesWithNamespacesResult {
    var importFrenchInNs: ImportLanguage? = null
    importBuilder
      .addImportFile {
        name = "file.json"
        namespace = "homepage"
        detectedNamespace = "homepage"
      }.build {
        addImportLanguage {
          name = "fr"
          existingLanguage = french
          importFrenchInNs = this
        }.build addFrLang@{
          addImportKey {
            name = "what a key with a namespace"
          }.build addImportKey@{
            addImportTranslation {
              language = this@addFrLang.self
              this.key = this@addImportKey.self
              text = "Texto text"
            }
          }
        }
      }
    importBuilder.addImportFile {
      name = "file2.json"
      namespace = "homepage"
    }
    return AddFilesWithNamespacesResult(importFrenchInNs!!)
  }

  fun addPluralImport() {
    this.projectBuilder.build {
      addKey {
        name = "plural key"
        isPlural = true
        pluralArgName = "count"
      }
      importBuilder.data.importFiles[0].build {
        val key =
          addImportKey {
            name = "plural key"
          }
        addImportTranslation {
          text = "Hey!"
          this.key = key.self
          language = importEnglish
        }
      }
    }
  }

  fun addImportKeyThatDoesntExistInProject() {
    importBuilder.data.importFiles[0].build {
      val key =
        addImportKey {
          name = "I'm new key in project"
        }
      addImportTranslation {
        text = "Hey!"
        this.key = key.self
        language = importEnglish
      }
    }
  }

  fun useCzechBaseLanguage() {
    project.baseLanguage = czech
  }

  fun addBranch() {
    this.projectBuilder.apply {
      featureBranch =
        addBranch {
          name = "feature"
          project = projectBuilder.self
          isProtected = false
          isDefault = false
          originBranch = this
        }.self
    }
  }

  /**
   * Sets up the scenario from production bug TOLGEE-BACKEND-3E9:
   *
   * Two import files both mapping to the same existing language, both containing the same key.
   * File 1 has text different from DB → the Translation entity gets mutated in-memory by
   * setTranslationTextNoSave during doImport(). File 2 has text equal to the *original* DB text,
   * but after the mutation the existingTranslations cache shows the new text — so handleConflicts
   * sees file 2's text as "different" and sets conflict instead of removing it, then if
   * isSelectedToImport is still true a second Translation INSERT is attempted, causing:
   *   PSQLException: duplicate key value violates unique constraint "translation_key_language"
   *
   * Both import translations are intentionally left with isSelectedToImport=true (the default) to
   * simulate the state before resetCollisionsBetweenFiles is applied, or when it fails to catch
   * the collision.
   *
   * @return the existing DB Translation that must NOT be duplicated
   */
  fun addTwoFilesWithSameExistingKey(): ExistingKeyScenario {
    val existingKey = projectBuilder.addKey { name = "duplicate-candidate-key" }.self
    val existingTranslation =
      projectBuilder
        .addTranslation {
          this.key = existingKey
          this.language = english
          this.text = "Original DB text"
        }.self

    // File 1: same key, DIFFERENT text → will UPDATE the translation entity in-memory during doImport()
    val file1ImportLang =
      importBuilder
        .addImportFile { name = "file1.json" }
        .build {
          addImportLanguage {
            name = "en"
            existingLanguage = english
          }.build {
            val importKey = addImportKey { name = "duplicate-candidate-key" }
            addImportTranslation {
              this.language = this@build.self
              this.key = importKey.self
              this.text = "Updated text from file 1"
              // conflict intentionally NOT pre-set — handleConflicts() must detect it
            }
          }
        }.data
        .importLanguages[0]
        .self

    // File 2: same key, text equal to ORIGINAL DB text.
    // After file 1's doImport() mutates the entity, handleConflicts() in file 2's prepareImport()
    // will see "Updated text from file 1" in the entity and treat file 2's text as "different",
    // causing it to set conflict=entity rather than removing it.  If isSelectedToImport stays true
    // it will attempt a second INSERT → constraint violation.
    val file2ImportLang =
      importBuilder
        .addImportFile { name = "file2.json" }
        .build {
          addImportLanguage {
            name = "en"
            existingLanguage = english
          }.build {
            val importKey = addImportKey { name = "duplicate-candidate-key" }
            addImportTranslation {
              this.language = this@build.self
              this.key = importKey.self
              this.text = "Original DB text"
              // conflict intentionally NOT pre-set
            }
          }
        }.data
        .importLanguages[0]
        .self

    return ExistingKeyScenario(existingTranslation, file1ImportLang, file2ImportLang)
  }

  /**
   * Sets up the scenario that directly triggers TOLGEE-BACKEND-3E9:
   *
   * Two import files both mapped to the same existing language, both containing the same
   * BRAND-NEW key (not present in the DB). Since existingTranslations has no entry for
   * this key, handleConflicts() leaves conflict=null on both ImportTranslations.
   * When resetCollisionsBetweenFiles has not run (or failed to deselect one), both
   * ImportTranslation.doImport() calls reach the `this.conflict ?: Translation()` branch
   * and create two separate Translation entities for the same (key_id, language_id) pair.
   * Saving both issues two INSERTs → PSQLException: duplicate key value violates unique
   * constraint "translation_key_language".
   *
   * Both import translations are intentionally left with isSelectedToImport=true (default).
   *
   * @return the two import language handles (so tests can inspect them)
   */
  fun addTwoFilesWithSameNewKey(): NewKeyScenario {
    val file1ImportLang =
      importBuilder
        .addImportFile { name = "new-f1.json" }
        .build {
          addImportLanguage {
            name = "en"
            existingLanguage = english
          }.build {
            val importKey = addImportKey { name = "brand-new-duplicate-key" }
            addImportTranslation {
              this.language = this@build.self
              this.key = importKey.self
              this.text = "Text from file 1"
            }
          }
        }.data
        .importLanguages[0]
        .self

    val file2ImportLang =
      importBuilder
        .addImportFile { name = "new-f2.json" }
        .build {
          addImportLanguage {
            name = "en"
            existingLanguage = english
          }.build {
            val importKey = addImportKey { name = "brand-new-duplicate-key" }
            addImportTranslation {
              this.language = this@build.self
              this.key = importKey.self
              this.text = "Text from file 2"
            }
          }
        }.data
        .importLanguages[0]
        .self

    return NewKeyScenario(file1ImportLang, file2ImportLang)
  }

  data class ExistingKeyScenario(
    val existingTranslation: io.tolgee.model.translation.Translation,
    val file1ImportLanguage: ImportLanguage,
    val file2ImportLanguage: ImportLanguage,
  )

  data class NewKeyScenario(
    val file1ImportLanguage: ImportLanguage,
    val file2ImportLanguage: ImportLanguage,
  )

  data class AddFilesWithNamespacesResult(
    val importFrenchInNs: ImportLanguage,
  )
}
