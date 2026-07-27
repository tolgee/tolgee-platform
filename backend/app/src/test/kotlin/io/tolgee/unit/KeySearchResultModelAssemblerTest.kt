package io.tolgee.unit

import io.tolgee.hateoas.key.KeySearchResultModelAssembler
import io.tolgee.service.key.KeySearchResultView
import io.tolgee.service.security.SecurityService
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class KeySearchResultModelAssemblerTest {
  private val securityService = Mockito.mock(SecurityService::class.java)

  private val underTest = KeySearchResultModelAssembler(securityService)

  private fun view(deletedByUserUsername: String?): KeySearchResultView {
    val view = mock<KeySearchResultView>()
    whenever(view.deletedByUserUsername).thenReturn(deletedByUserUsername)
    return view
  }

  @Test
  fun `exposes the address the security service returns`() {
    whenever(securityService.maskedMemberField("jane@tolgee.io")).thenReturn("jane@tolgee.io")

    underTest
      .toModel(view("jane@tolgee.io"))
      .deletedByUserUsername.assert
      .isEqualTo("jane@tolgee.io")
  }

  @Test
  fun `serves the masked value, not the one the delegated view carries`() {
    whenever(securityService.maskedMemberField("jane@tolgee.io")).thenReturn("")

    underTest
      .toModel(view("jane@tolgee.io"))
      .deletedByUserUsername.assert
      .isEqualTo("")
  }

  @Test
  fun `leaves a null address null without consulting the security service`() {
    underTest
      .toModel(view(null))
      .deletedByUserUsername.assert
      .isNull()

    verify(securityService, never()).maskedMemberField(any())
  }
}
