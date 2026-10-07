package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.key.Key

class KeyUniquenessTestData : BaseTestData() {
  val keyWithoutNamespace: Key = projectBuilder.addKey { name = "duplicated" }.self
  val keyInNamespace: Key = projectBuilder.addKey(keyName = "duplicated", namespace = "homepage").self
}
