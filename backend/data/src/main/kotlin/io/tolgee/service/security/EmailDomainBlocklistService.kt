package io.tolgee.service.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Service

@Service
class EmailDomainBlocklistService(
  private val tolgeeProperties: TolgeeProperties,
) {
  // Bundled snapshot of github.com/disposable-email-domains/disposable-email-domains.
  // Re-vendor by overwriting the resource file with the upstream list.
  private val bundledDomains: Set<String> by lazy { loadBundledDomains() }

  fun isBlocked(domain: String): Boolean {
    val normalized = domain.lowercase()
    val authentication = tolgeeProperties.authentication
    if (normalized in authentication.allowedEmailDomains.toLowerCaseSet()) {
      return false
    }
    if (normalized in authentication.blockedEmailDomains.toLowerCaseSet()) {
      return true
    }
    return authentication.blockDisposableEmails && normalized in bundledDomains
  }

  private fun Collection<String>.toLowerCaseSet(): Set<String> = this.mapTo(mutableSetOf()) { it.lowercase() }

  private fun loadBundledDomains(): Set<String> {
    val stream =
      javaClass.getResourceAsStream(BUNDLED_LIST_RESOURCE)
        ?: return emptySet()
    return stream.bufferedReader().useLines { lines ->
      lines
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .toSet()
    }
  }

  companion object {
    private const val BUNDLED_LIST_RESOURCE = "/email/disposable-email-domains.txt"
  }
}
