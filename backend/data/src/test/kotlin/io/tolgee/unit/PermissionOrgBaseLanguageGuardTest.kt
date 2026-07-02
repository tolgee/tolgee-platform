package io.tolgee.unit

import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PermissionOrgBaseLanguageGuardTest {
  private val listeners = Permission.Companion.PermissionListeners()

  private fun orgBasePermission() =
    Permission().apply {
      organization = Organization()
    }

  @Test
  fun `rejects org base permission with suggest languages`() {
    val permission =
      orgBasePermission().apply {
        suggestLanguages = mutableSetOf(Language())
      }
    assertThatThrownBy { listeners.prePersist(permission) }
      .isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `rejects org base permission with suggest-manage languages`() {
    val permission =
      orgBasePermission().apply {
        suggestManageLanguages = mutableSetOf(Language())
      }
    assertThatThrownBy { listeners.prePersist(permission) }
      .isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `allows org base permission without language restrictions`() {
    assertThatCode { listeners.prePersist(orgBasePermission()) }
      .doesNotThrowAnyException()
  }
}
