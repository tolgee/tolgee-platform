package io.tolgee.development.testDataBuilder

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder

interface AdditionalTestDataSaver {
  fun save(builder: TestDataBuilder)

  fun clean(builder: TestDataBuilder)

  fun before(builder: TestDataBuilder) {}

  fun after(builder: TestDataBuilder) {}
}
