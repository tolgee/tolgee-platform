package io.tolgee.unit

import io.tolgee.hateoas.contributor.ContributorModelAssembler
import io.tolgee.model.views.ProjectContributorView
import io.tolgee.service.AvatarService
import io.tolgee.service.security.SecurityService
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Date

class ContributorModelAssemblerTest {
  private val avatarService = Mockito.mock(AvatarService::class.java)
  private val securityService = Mockito.mock(SecurityService::class.java)

  private val underTest = ContributorModelAssembler(avatarService, securityService)

  private fun view(username: String): ProjectContributorView {
    val view = mock<ProjectContributorView>()
    whenever(view.id).thenReturn(1L)
    whenever(view.username).thenReturn(username)
    whenever(view.name).thenReturn("Cora Contributor")
    whenever(view.firstContributionAt).thenReturn(Date(1_600_000_000_000))
    whenever(view.lastContributionAt).thenReturn(Date(1_600_000_100_000))
    return view
  }

  @Test
  fun `exposes the address the security service returns`() {
    whenever(securityService.maskedMemberField("cora@tolgee.io")).thenReturn("cora@tolgee.io")

    underTest
      .toModel(view("cora@tolgee.io"))
      .username.assert
      .isEqualTo("cora@tolgee.io")
  }

  @Test
  fun `serves the masked value, not the address the view carries`() {
    whenever(securityService.maskedMemberField("cora@tolgee.io")).thenReturn("")

    underTest
      .toModel(view("cora@tolgee.io"))
      .username.assert
      .isEqualTo("")
  }
}
