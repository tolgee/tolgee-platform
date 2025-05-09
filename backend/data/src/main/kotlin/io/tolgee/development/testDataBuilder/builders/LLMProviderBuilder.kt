package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.LLMProvider

class LLMProviderBuilder(
  val organizationBuilder: OrganizationBuilder,
) : BaseEntityDataBuilder<LLMProvider, LLMProviderBuilder>() {
  override var self: LLMProvider =
    LLMProvider(
      organization = organizationBuilder.self,
    )
}
