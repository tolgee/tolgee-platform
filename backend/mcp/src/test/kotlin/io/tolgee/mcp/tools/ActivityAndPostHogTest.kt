package io.tolgee.mcp.tools

import io.tolgee.activity.data.ActivityType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class ActivityAndPostHogTest : McpSecurityTestBase() {
  @Test
  fun `activity type set assigns to activityHolder`() {
    sut.executeAs(spec(activityType = ActivityType.CREATE_KEY)) {}

    verify(activityHolder).activity = ActivityType.CREATE_KEY
  }

  @Test
  fun `activity type null does not set activity`() {
    sut.executeAs(spec(activityType = null)) {}

    verify(activityHolder, never()).activity = any()
  }

  @Test
  fun `businessEventData always gets mcp and mcp_operation`() {
    sut.executeAs(spec(mcpOperation = "search_keys")) {}

    assertThat(businessEventData["mcp"]).isEqualTo("true")
    assertThat(businessEventData["mcp_operation"]).isEqualTo("search_keys")
  }
}
