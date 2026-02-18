package io.tolgee.ee.service

import io.tolgee.exceptions.LlmProviderNotFoundException
import io.tolgee.repository.LlmProviderRepository
import io.tolgee.service.LlmPropertiesService
import org.springframework.stereotype.Component

@Component
class LlmProviderResolver(
  private val llmProviderRepository: LlmProviderRepository,
  private val llmPropertiesService: LlmPropertiesService,
) {
  fun resolveProviderName(
    organizationId: Long,
    provider: String,
  ): String {
    var current = provider
    val tried = mutableSetOf<String>()
    repeat(MAX_FALLBACK_DEPTH) {
      if (providerExists(organizationId, current)) {
        return current
      }
      tried.add(current)
      val fallback = llmPropertiesService.getFallbackProviderName(current)
      if (fallback == null || fallback in tried) {
        throw LlmProviderNotFoundException(provider)
      }
      current = fallback
    }
    throw LlmProviderNotFoundException(provider)
  }

  private fun providerExists(
    organizationId: Long,
    name: String,
  ): Boolean {
    if (llmProviderRepository.getAll(organizationId).any { it.name == name }) return true
    return llmPropertiesService.getProviders().any { it.name == name }
  }

  companion object {
    private const val MAX_FALLBACK_DEPTH = 10
  }
}
