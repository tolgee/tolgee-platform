package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.DisableManagedUserTestData
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.events.OnUserCountChanged
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.UserDisabledBy
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
    testData = null
  }

  @Test
  fun `disable publishes one decrease event on a real transition`() {
    val testData = saveTestData()
    val events =
      captureUserCountEvents { userAccountService.disable(testData.managedMember.id, UserDisabledBy.ORGANIZATION) }
    assertThat(events).hasSize(1)
    assertThat(events[0].decrease).isTrue()
  }

  @Test
  fun `enable publishes one increase event on a real transition`() {
    val testData = saveTestData()
    userAccountService.disable(testData.managedMember.id, UserDisabledBy.ORGANIZATION)
    val events =
      captureUserCountEvents { userAccountService.enable(testData.managedMember.id, UserDisabledBy.ORGANIZATION) }
    assertThat(events).hasSize(1)
    assertThat(events[0].decrease).isFalse()
  }

  @Test
  fun `an organization cannot enable a user an admin disabled`() {
    val testData = saveTestData()
    userAccountService.disable(testData.managedMember.id, UserDisabledBy.ADMIN)
    assertThrows<ValidationException> {
      userAccountService.enable(testData.managedMember.id, UserDisabledBy.ORGANIZATION)
    }
    assertThat(userAccountService.findActiveOrDisabled(testData.managedMember.id)!!.disabledAt).isNotNull()
  }

  @Test
  fun `disabling an already-disabled user publishes no event`() {
    val testData = saveTestData()
    val events =
      captureUserCountEvents {
        userAccountService.disable(testData.disabledNonManagedMember.id, UserDisabledBy.ORGANIZATION)
      }
    assertThat(events).isEmpty()
  }

  @Test
  fun `enabling an already-enabled user publishes no event`() {
    val testData = saveTestData()
    val events = captureUserCountEvents { userAccountService.enable(testData.managedMember.id, UserDisabledBy.ADMIN) }
    assertThat(events).isEmpty()
  }

  @Test
  fun `an admin takeover of an org disable publishes no event`() {
    val testData = saveTestData()
    userAccountService.disable(testData.managedMember.id, UserDisabledBy.ORGANIZATION)
    val events =
      captureUserCountEvents { userAccountService.disable(testData.managedMember.id, UserDisabledBy.ADMIN) }
    assertThat(events).isEmpty()
  }

  @Test
  fun `an admin can still disable a platform admin an organization may not touch`() {
    val testData = saveTestData()
    assertThrows<ValidationException> {
      userAccountService.disable(testData.managedPlatformAdmin.id, UserDisabledBy.ORGANIZATION)
    }
    userAccountService.disable(testData.managedPlatformAdmin.id, UserDisabledBy.ADMIN)
    assertThat(userAccountService.findActiveOrDisabled(testData.managedPlatformAdmin.id)!!.disabledAt).isNotNull()
  }

  @Test
  fun `an organization cannot enable an account whose disable origin is unknown`() {
    val testData = saveTestData()
    assertThrows<ValidationException> {
      userAccountService.enable(testData.nullOriginDisabledManagedMember.id, UserDisabledBy.ORGANIZATION)
    }
    val user = userAccountService.findActiveOrDisabled(testData.nullOriginDisabledManagedMember.id)!!
    assertThat(user.disabledAt).isNotNull()
    assertThat(user.disabledBy).isNull()
  }

  @Test
  fun `an organization disable does not claim an account whose disable origin is unknown`() {
    val testData = saveTestData()
    val events =
      captureUserCountEvents {
        userAccountService.disable(testData.nullOriginDisabledManagedMember.id, UserDisabledBy.ORGANIZATION)
      }
    assertThat(events).isEmpty()
    assertThat(userAccountService.findActiveOrDisabled(testData.nullOriginDisabledManagedMember.id)!!.disabledBy)
      .isNull()
  }

  @Test
  fun `an admin can enable an account whose disable origin is unknown`() {
    val testData = saveTestData()
    userAccountService.enable(testData.nullOriginDisabledManagedMember.id, UserDisabledBy.ADMIN)
    assertThat(userAccountService.findActiveOrDisabled(testData.nullOriginDisabledManagedMember.id)!!.disabledAt)
      .isNull()
  }

  @Test
  fun `disable throws NotFound for a nonexistent user`() {
    assertThrows<NotFoundException> { userAccountService.disable(NONEXISTENT_USER_ID, UserDisabledBy.ADMIN) }
  }

  @Test
  fun `enable throws NotFound for a nonexistent user`() {
    assertThrows<NotFoundException> { userAccountService.enable(NONEXISTENT_USER_ID, UserDisabledBy.ADMIN) }
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
