package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.LlmProvider

class LlmProviderBuilder(
  val organizationBuilder: OrganizationBuilder,
) : BaseEntityDataBuilder<LlmProvider, LlmProviderBuilder>() {
  override var self: LlmProvider =
    LlmProvider(
      organization = organizationBuilder.self,
    )
}
