package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import io.tolgee.model.keyBigMeta.BigMeta

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
        contextData = mapOf("random data a" to "haha")
      }.self

      addBigMeta {
        namespace = "yep"
        keyName = "key"
        location = "home"
        contextData = mapOf("random data" to "haha")
      }
      addBigMeta {
        namespace = "yep"
        keyName = "hups"
        location = "home"
        contextData = listOf("random data")
      }
      addBigMeta {
        namespace = "yep"
        keyName = "hups"
        location = "not-home"
        contextData = listOf("random data")
      }
    }
  }
}
