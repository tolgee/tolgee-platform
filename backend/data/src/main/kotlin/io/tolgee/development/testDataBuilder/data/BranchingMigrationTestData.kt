package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.dataImport.Import
import io.tolgee.model.key.Key
import io.tolgee.model.task.Task

class BranchingMigrationTestData : BaseTestData("branching_migration", "Branching migration data") {
  var language: Language
  lateinit var key: Key
  lateinit var task: Task
  lateinit var importEntity: Import

  init {
    projectBuilder.self.useBranching = false
    language = englishLanguage

    projectBuilder.apply {
      addKey {
        name = "legacy-key"
      }.build {
        key = self
      }
      addTask {
        name = "legacy-task"
        this.language = this@BranchingMigrationTestData.language
      }.build {
        task = self
      }
      addImport {
        author = user
      }.build {
        importEntity = self
      }
    }
  }
}
