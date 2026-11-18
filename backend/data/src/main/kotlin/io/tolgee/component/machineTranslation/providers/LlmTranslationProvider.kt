package io.tolgee.component.machineTranslation.providers

abstract class LlmTranslationProvider : AbstractMtValueProvider() {
  open fun consumesCredits(
    organizationId: Long,
    provider: String,
  ): Boolean = true
}
