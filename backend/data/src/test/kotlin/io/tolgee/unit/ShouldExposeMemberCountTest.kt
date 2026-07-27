package io.tolgee.unit

import io.tolgee.security.ProjectHolder
import io.tolgee.service.security.SecurityService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

class ShouldExposeMemberCountTest {
  @Test
  fun `shouldExposeMemberCount is true off-request without touching the project holder`() {
    val projectHolder =
      mock<ProjectHolder> {
        whenever(it.projectOrNull).thenThrow(IllegalStateException("project holder must not be read off-request"))
      }
    val securityService =
      SecurityService(mock(), mock(), mock(), projectHolder = projectHolder, branchService = mock())

    assertThat(securityService.shouldExposeMemberCount()).isTrue()
  }

  @Test
  fun `shouldExposeMemberCount is true when no project is in scope`() {
    val projectHolder = mock<ProjectHolder> { whenever(it.projectOrNull).thenReturn(null) }
    val securityService =
      SecurityService(mock(), mock(), mock(), projectHolder = projectHolder, branchService = mock())

    RequestContextHolder.setRequestAttributes(mock<RequestAttributes>())
    try {
      assertThat(securityService.shouldExposeMemberCount()).isTrue()
    } finally {
      RequestContextHolder.resetRequestAttributes()
    }
  }
}
