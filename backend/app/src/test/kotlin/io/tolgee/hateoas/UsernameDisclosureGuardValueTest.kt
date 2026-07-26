package io.tolgee.hateoas

import io.tolgee.hateoas.activity.ProjectActivityAuthorModel
import io.tolgee.hateoas.apiKey.ApiKeyModel
import io.tolgee.hateoas.apiKey.ApiKeyWithLanguagesModel
import io.tolgee.hateoas.apiKey.RevealedApiKeyModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.testing.security.UsernameDisclosureGuard
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

@Suppress("DEPRECATION")
class UsernameDisclosureGuardValueTest {
  private class DeclaresUsername(
    @Suppress("unused") val username: String,
  )

  private class NoUsername(
    @Suppress("unused") val name: String,
  )

  private class RenamedAddress(
    @Suppress("unused") val authorEmail: String?,
    @Suppress("unused") val contactEMail: String?,
    @Suppress("unused") val authorUsername: String?,
    @Suppress("unused") val email: Boolean,
    @Suppress("unused") val invitedUserName: String?,
    @Suppress("unused") val name: String,
  )

  @Test
  fun `suspects address-bearing properties whatever they are called, ignoring non-address types`() {
    assertThat(UsernameDisclosureGuard.userAddressProperties(RenamedAddress::class))
      .containsExactlyInAnyOrder("authorEmail", "contactEMail", "authorUsername")
  }

  @Test
  fun `declaresUsername detects a username property`() {
    assertThat(UsernameDisclosureGuard.declaresUsername(DeclaresUsername::class)).isTrue
    assertThat(UsernameDisclosureGuard.declaresUsername(NoUsername::class)).isFalse
  }

  @Test
  fun `every stripped model exposes no username`() {
    val apiKeyModel = ApiKeyModel(id = 1L, description = "desc")
    val instances =
      mapOf<KClass<*>, String?>(
        SimpleUserAccountModel::class to
          SimpleUserAccountModel(id = 1L, name = "Name", avatar = null, deleted = false).username,
        ProjectActivityAuthorModel::class to
          ProjectActivityAuthorModel(id = 1L, name = "Name", avatar = null, deleted = false).username,
        ApiKeyModel::class to apiKeyModel.username,
        RevealedApiKeyModel::class to RevealedApiKeyModel(apiKeyModel).username,
        ApiKeyWithLanguagesModel::class to ApiKeyWithLanguagesModel(apiKeyModel, permittedLanguageIds = null).username,
      )

    assertThat(instances.values).allSatisfy { assertThat(it).isEmpty() }

    assertThat(instances.keys.mapNotNull { it.qualifiedName })
      .`as`("every strippedModelName must be value-asserted here, else a stripped model could re-leak silently")
      .containsExactlyInAnyOrderElementsOf(UsernameDisclosureGuard.strippedModelNames)
  }
}
