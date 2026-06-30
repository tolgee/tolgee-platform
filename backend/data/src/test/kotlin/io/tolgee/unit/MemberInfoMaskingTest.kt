package io.tolgee.unit

import io.tolgee.security.ProjectHolder
import io.tolgee.service.security.SecurityService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MemberInfoMaskingTest {
  @Test
  fun `maskedMemberField exposes the value off-request without touching the project holder`() {
    val projectHolder =
      mock<ProjectHolder> {
        whenever(it.projectOrNull).thenThrow(IllegalStateException("project holder must not be read off-request"))
      }
    val securityService =
      SecurityService(mock(), mock(), mock(), projectHolder = projectHolder, branchService = mock())

    assertThat(securityService.shouldExposeMemberInfo()).isTrue()
    assertThat(securityService.maskedMemberField("translator@test.com")).isEqualTo("translator@test.com")
  }
}
