package io.tolgee.mcp.tools

import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever

class CheckReadOnlyModeTest : McpSecurityTestBase() {
  @Test
  fun `write op with readOnly auth throws OPERATION_NOT_PERMITTED_IN_READ_ONLY_MODE`() {
    whenever(authenticationFacade.isReadOnly).thenReturn(true)

    assertThatThrownBy {
      sut.executeAs(spec(isWriteOperation = true)) {}
    }.isInstanceOf(PermissionException::class.java)
      .satisfies({ ex ->
        assertThat((ex as PermissionException).tolgeeMessage)
          .isEqualTo(Message.OPERATION_NOT_PERMITTED_IN_READ_ONLY_MODE)
      })
  }

  @Test
  fun `read op with readOnly auth passes`() {
    whenever(authenticationFacade.isReadOnly).thenReturn(true)

    sut.executeAs(spec(isWriteOperation = false)) {}
  }

  @Test
  fun `write op with non-readOnly auth passes`() {
    whenever(authenticationFacade.isReadOnly).thenReturn(false)

    sut.executeAs(spec(isWriteOperation = true)) {}
  }
}
