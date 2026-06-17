package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.DisableManagedUserTestData
import io.tolgee.events.OnUserCountChanged
import io.tolgee.exceptions.NotFoundException
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ApplicationEventMulticaster

class UserAccountDisableEnableEventTest : AbstractSpringTest() {
  private var testData: DisableManagedUserTestData? = null

  @AfterEach
  fun cleanData() {
    testData?.let { testDataService.cleanTestData(it.root) }
  }

  @Test
  fun `disable publishes one decrease event on a real transition`() {
    val testData = saveTestData()
    val events = captureUserCountEvents { userAccountService.disable(testData.managedMember.id) }
    assertThat(events).hasSize(1)
    assertThat(events[0].decrease).isTrue()
  }

  @Test
  fun `enable publishes one increase event on a real transition`() {
    val testData = saveTestData()
    userAccountService.disable(testData.managedMember.id)
    val events = captureUserCountEvents { userAccountService.enable(testData.managedMember.id) }
    assertThat(events).hasSize(1)
    assertThat(events[0].decrease).isFalse()
  }

  @Test
  fun `disabling an already-disabled user publishes no event`() {
    val testData = saveTestData()
    val events = captureUserCountEvents { userAccountService.disable(testData.disabledNonManagedMember.id) }
    assertThat(events).isEmpty()
  }

  @Test
  fun `enabling an already-enabled user publishes no event`() {
    val testData = saveTestData()
    val events = captureUserCountEvents { userAccountService.enable(testData.managedMember.id) }
    assertThat(events).isEmpty()
  }

  @Test
  fun `disable throws NotFound for a nonexistent user`() {
    assertThrows<NotFoundException> { userAccountService.disable(NONEXISTENT_USER_ID) }
  }

  @Test
  fun `enable throws NotFound for a nonexistent user`() {
    assertThrows<NotFoundException> { userAccountService.enable(NONEXISTENT_USER_ID) }
  }

  private fun saveTestData(): DisableManagedUserTestData {
    val data = DisableManagedUserTestData()
    testDataService.saveTestData(data.root)
    testData = data
    return data
  }

  private fun captureUserCountEvents(block: () -> Unit): List<OnUserCountChanged> {
    val captured = mutableListOf<OnUserCountChanged>()
    val listener =
      ApplicationListener<ApplicationEvent> { event ->
        if (event is OnUserCountChanged) captured.add(event)
      }
    val multicaster = applicationContext.getBean(ApplicationEventMulticaster::class.java)
    multicaster.addApplicationListener(listener)
    try {
      block()
    } finally {
      multicaster.removeApplicationListener(listener)
    }
    return captured
  }

  companion object {
    private const val NONEXISTENT_USER_ID = -1L
  }
}
