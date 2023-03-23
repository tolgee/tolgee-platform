package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.keyBigMeta.BigMeta

class BigMetaBuilder(
  val projectBuilder: ProjectBuilder
) : EntityDataBuilder<BigMeta, BigMetaBuilder> {
  override var self: BigMeta = BigMeta().apply {
    project = projectBuilder.self
  }
}
