package io.tolgee.ee.unit.aiReview

import io.tolgee.ee.service.aiReview.matching.KeyScope
import io.tolgee.ee.service.aiReview.matching.RuleScopeMatcher
import io.tolgee.ee.service.aiReview.matching.RuleSnapshot
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class RuleScopeMatcherTest {
  private fun matches(
    rule: RuleSnapshot,
    key: KeyScope = key(),
    languageId: Long = DE,
    branchingEnabled: Boolean = true,
  ) = RuleScopeMatcher.matches(rule, key, languageId, branchingEnabled)

  @Test
  fun `empty rule matches everything`() {
    matches(rule()).assert.isTrue()
  }

  @Test
  fun `disabled rule never matches`() {
    matches(rule(enabled = false)).assert.isFalse()
  }

  @Test
  fun `language filter`() {
    matches(rule(languageIds = setOf(FR)), languageId = DE).assert.isFalse()
    matches(rule(languageIds = setOf(FR)), languageId = FR).assert.isTrue()
  }

  @Test
  fun `tagIn matches any listed tag exactly`() {
    matches(rule(tagIn = setOf("legal", "ui"))).assert.isTrue()
    matches(rule(tagIn = setOf("u*"))).assert.isFalse()
  }

  @Test
  fun `tagIn with empty string matches keys without tags only`() {
    matches(rule(tagIn = setOf("")), key(tags = emptySet())).assert.isTrue()
    matches(rule(tagIn = setOf("")), key(tags = setOf("ui"))).assert.isFalse()
  }

  @Test
  fun `tagNotIn excludes listed tags`() {
    matches(rule(tagNotIn = setOf("ui"))).assert.isFalse()
    matches(rule(tagNotIn = setOf("legal"))).assert.isTrue()
  }

  @Test
  fun `tagNotIn with empty string excludes keys without tags`() {
    matches(rule(tagNotIn = setOf("")), key(tags = emptySet())).assert.isFalse()
    matches(rule(tagNotIn = setOf("")), key(tags = setOf("ui"))).assert.isTrue()
  }

  @Test
  fun `empty string namespace is the default namespace`() {
    matches(rule(namespaceIn = setOf("")), key(namespace = "")).assert.isTrue()
    matches(rule(namespaceIn = setOf("")), key(namespace = "checkout")).assert.isFalse()
  }

  @Test
  fun `namespaceNotIn excludes listed namespaces`() {
    matches(rule(namespaceNotIn = setOf("checkout")), key(namespace = "checkout")).assert.isFalse()
    matches(rule(namespaceNotIn = setOf("checkout")), key(namespace = "")).assert.isTrue()
  }

  @Test
  fun `branch patterns apply when branching is enabled`() {
    matches(rule(branchIn = listOf("feature/*")), key(branchName = "main")).assert.isFalse()
    matches(rule(branchIn = listOf("feature/*")), key(branchName = "feature/x")).assert.isTrue()
    matches(rule(branchNotIn = listOf("draft/*")), key(branchName = "draft/x")).assert.isFalse()
  }

  @Test
  fun `branch patterns are ignored when branching is disabled`() {
    matches(
      rule(branchIn = listOf("feature/*")),
      key(branchName = "main"),
      branchingEnabled = false,
    ).assert.isTrue()
  }
}
