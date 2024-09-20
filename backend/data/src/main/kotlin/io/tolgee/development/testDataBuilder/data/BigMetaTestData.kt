package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import io.tolgee.service.bigMeta.BigMetaService
import kotlin.math.abs

class BigMetaTestData {
  lateinit var project: Project
  lateinit var projectBuilder: ProjectBuilder
  lateinit var userAccount: UserAccount
  lateinit var noNsKey: Key
  lateinit var yepKey: Key

  val root =
    TestDataBuilder().apply {
      addUserAccount {
        username = "hehe@hehe"
        userAccount = this
      }
      projectBuilder =
        addProject {
          name = "Project"
          this@BigMetaTestData.project = this
        }.build {
          noNsKey = addKey(null, "key").self
          yepKey = addKey("yep", "key").self
        }
    }

  fun addLotOfData(): List<Key> {
    val keys =
      (0..5000).map {
        projectBuilder.addKey(null, "key$it").self
      }
    return keys
  }

  fun addLotOfReferences(keys: List<Key>) {
    keys.forEachIndexed forEach1@{ idx1, key1 ->
      keys.forEachIndexed forEach2@{ idx2, key2 ->
        if (idx1 >= idx2 || abs(idx1 - idx2) > (BigMetaService.MAX_ORDER_DISTANCE + 1)) return@forEach2
        projectBuilder.addKeysDistance(key1, key2) {
          score = 10000
          hits = 1
        }
      }
    }
  }
}
