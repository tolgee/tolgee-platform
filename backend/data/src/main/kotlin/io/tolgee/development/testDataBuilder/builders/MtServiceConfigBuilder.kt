package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.MtServiceConfig

class MtServiceConfigBuilder(
  val projectBuilder: ProjectBuilder
) : BaseEntityDataBuilder<MtServiceConfig, MtServiceConfigBuilder>() {
  override var self: MtServiceConfig = MtServiceConfig()
    .apply {
      project = projectBuilder.self
    }
}
