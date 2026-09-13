package io.tolgee.security.oauth2

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class OAuth2ScopesTest {
  @Test
  fun `changing the scope vocabulary is a breaking wire change, so it has to be edited here too`() {
    // scopes_supported is published in RFC 8414 and RFC 9728 discovery and hard-coded by clients into authorize
    // requests, while Scope.value is a `var`. Spelling the list out is the point: adding, removing or renaming a
    // value is a breaking wire change, so it has to be a deliberate edit here rather than a side effect of the enum.
    OAuth2Scopes.SUPPORTED.assert.containsExactlyInAnyOrder(
      "translations.view",
      "translations.edit",
      "translations.suggest",
      "translation-suggestions.manage",
      "keys.edit",
      "screenshots.upload",
      "screenshots.delete",
      "screenshots.view",
      "activity.view",
      "languages.edit",
      "admin",
      "project.edit",
      "members.view",
      "members.edit",
      "translation-comments.add",
      "translation-comments.edit",
      "translation-comments.set-state",
      "translations.state-edit",
      "keys.view",
      "keys.delete",
      "keys.create",
      "batch-jobs.view",
      "batch-jobs.cancel",
      "translations.batch-by-tm",
      "translations.batch-machine",
      "content-delivery.manage",
      "content-delivery.publish",
      "webhooks.manage",
      "tasks.view",
      "tasks.edit",
      "tasks.assigned-access",
      "prompts.view",
      "prompts.edit",
      "translation-labels.manage",
      "translation-labels.assign",
      "all.view",
      "branch.management",
      "branch.protected-modify",
      "organization-quotas.view",
    )
  }

  @Test
  fun `a scope is supported only when it is one of Tolgee's own`() {
    OAuth2Scopes.isSupported("translations.view").assert.isTrue()
    OAuth2Scopes.isSupported("translations.edit").assert.isTrue()
    OAuth2Scopes.isSupported("not.a.scope").assert.isFalse()
    OAuth2Scopes.isSupported("TRANSLATIONS_VIEW").assert.isFalse()
  }
}
