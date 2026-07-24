package io.tolgee.hateoas

import io.tolgee.hateoas.activity.ProjectActivityAuthorModel
import io.tolgee.hateoas.apiKey.ApiKeyModel
import io.tolgee.hateoas.apiKey.ApiKeyWithLanguagesModel
import io.tolgee.hateoas.apiKey.RevealedApiKeyModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The guard only proves a stripped model *declares* username; it does not read the value. These
 * assertions pin the value contract for each stripped model — a future edit that populates username
 * (e.g. a computed getter returning the e-mail) fails here even though the guard scan stays green.
 */
class UsernameDisclosureGuardValueTest {
  @Test
  fun `SimpleUserAccountModel exposes no username`() {
    assertThat(SimpleUserAccountModel(id = 1L, name = "Name", avatar = null, deleted = false).username).isEmpty()
  }

  @Test
  fun `ProjectActivityAuthorModel exposes no username`() {
    assertThat(ProjectActivityAuthorModel(id = 1L, name = "Name", avatar = null, deleted = false).username).isEmpty()
  }

  @Test
  fun `ApiKeyModel and its delegating models expose no username`() {
    val apiKeyModel = ApiKeyModel(id = 1L, description = "desc")
    assertThat(apiKeyModel.username).isEmpty()
    assertThat(RevealedApiKeyModel(apiKeyModel).username).isEmpty()
    assertThat(ApiKeyWithLanguagesModel(apiKeyModel, permittedLanguageIds = null).username).isEmpty()
  }
}
