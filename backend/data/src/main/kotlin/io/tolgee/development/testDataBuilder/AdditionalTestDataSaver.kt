package io.tolgee.development.testDataBuilder

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder

interface AdditionalTestDataSaver {
  fun save(builder: TestDataBuilder)

  fun clean(builder: TestDataBuilder)

  fun beforeSave(builder: TestDataBuilder) {}

  fun afterSave(builder: TestDataBuilder) {}

  fun beforeClean(builder: TestDataBuilder) {}

  fun afterClean(builder: TestDataBuilder) {}
}
