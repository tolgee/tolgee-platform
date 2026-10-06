package io.tolgee.api.v2.controllers.configurationProps

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConfigurationDocumentationProviderTest {
  private val docs = ConfigurationDocumentationProvider().docs

  @Test
  fun `does not document read-only properties`() {
    assertThat(childNames("tolgee.content-delivery.cache-purging.cloudflare"))
      .contains("apiKey", "zoneId")
      .doesNotContain("enabled", "contentDeliveryCachePurgingType")
    assertThat(childNames("tolgee.content-delivery.cache-purging.bunny"))
      .contains("apiKey")
      .doesNotContain("enabled", "contentDeliveryCachePurgingType")
  }

  private fun childNames(prefix: String): List<String> =
    findGroup(docs, prefix)?.children?.map { it.name }
      ?: throw AssertionError("Group $prefix not found")

  private fun findGroup(
    items: List<DocItem>,
    prefix: String,
  ): Group? {
    val groups = items.filterIsInstance<Group>()
    groups.find { it.prefix == prefix }?.let { return it }
    return groups.firstNotNullOfOrNull { findGroup(it.children, prefix) }
  }
}
