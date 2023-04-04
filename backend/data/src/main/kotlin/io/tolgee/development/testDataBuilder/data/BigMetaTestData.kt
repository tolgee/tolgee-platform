package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import io.tolgee.model.keyBigMeta.BigMeta
import io.tolgee.model.keyBigMeta.SurroundingKey

class BigMetaTestData {
  lateinit var project: Project
  lateinit var userAccount: UserAccount
  lateinit var noNsKey: Key
  lateinit var yepKey: Key
  lateinit var someBigMeta: BigMeta

  val root = TestDataBuilder().apply {
    addUserAccount {
      username = "hehe@hehe"
      userAccount = this
    }
    addProject {
      name = "Project"
      this@BigMetaTestData.project = this
    }.build {
      noNsKey = addKey(null, "key").self
      yepKey = addKey("yep", "key").self

      someBigMeta = addBigMeta {
        namespace = null
        keyName = "key"
        location = "home"
        contextData = listOf(SurroundingKey("key", "yep"))
      }.self

      addBigMeta {
        namespace = "yep"
        keyName = "key"
        location = "home"
        contextData = listOf(SurroundingKey("key", null))
      }
      addBigMeta {
        namespace = "yep"
        keyName = "hups"
        location = "home"
        contextData = listOf(SurroundingKey("key", null))
      }
      addBigMeta {
        namespace = "yep"
        keyName = "hups"
        location = "not-home"
        contextData = listOf(SurroundingKey("key", null))
      }
    }
  }
}
