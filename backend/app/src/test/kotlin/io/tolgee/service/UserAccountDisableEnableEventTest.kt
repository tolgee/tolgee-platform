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
    val data = saveTestData()
    val events =
      captureUserCountEvents { userAccountService.disable(data.managedMember.id, UserDisabledBy.ORGANIZATION) }
    assertThat(events).hasSize(1)
    assertThat(events[0].decrease).isTrue()
  }

  @Test
  fun `enable publishes one increase event on a real transition`() {
    val data = saveTestData()
    userAccountService.disable(data.managedMember.id, UserDisabledBy.ORGANIZATION)
    val events =
      captureUserCountEvents { userAccountService.enable(data.managedMember.id, UserDisabledBy.ORGANIZATION) }
    assertThat(events).hasSize(1)
    assertThat(events[0].decrease).isFalse()
  }

  @Test
  fun `an organization cannot enable a user an admin disabled`() {
    val data = saveTestData()
    userAccountService.disable(data.managedMember.id, UserDisabledBy.ADMIN)
    assertThrows<ValidationException> {
      userAccountService.enable(data.managedMember.id, UserDisabledBy.ORGANIZATION)
    }
    assertThat(userAccountService.findActiveOrDisabled(data.managedMember.id)!!.disabledAt).isNotNull()
  }

  @Test
  fun `disabling an already-disabled user publishes no event`() {
    val data = saveTestData()
    val events =
      captureUserCountEvents {
        userAccountService.disable(data.orgDisabledManagedMember.id, UserDisabledBy.ORGANIZATION)
      }
    assertThat(events).isEmpty()
  }

  @Test
  fun `enabling an already-enabled user publishes no event`() {
    val data = saveTestData()
    val events = captureUserCountEvents { userAccountService.enable(data.managedMember.id, UserDisabledBy.ADMIN) }
    assertThat(events).isEmpty()
  }

  @Test
  fun `an admin takeover of an org disable publishes no event`() {
    val data = saveTestData()
    userAccountService.disable(data.managedMember.id, UserDisabledBy.ORGANIZATION)
    val events =
      captureUserCountEvents { userAccountService.disable(data.managedMember.id, UserDisabledBy.ADMIN) }
    assertThat(events).isEmpty()
  }

  @Test
  fun `an admin can still disable a platform admin an organization may not touch`() {
    val data = saveTestData()
    assertThrows<ValidationException> {
      userAccountService.disable(data.managedPlatformAdmin.id, UserDisabledBy.ORGANIZATION)
    }
    userAccountService.disable(data.managedPlatformAdmin.id, UserDisabledBy.ADMIN)
    assertThat(userAccountService.findActiveOrDisabled(data.managedPlatformAdmin.id)!!.disabledAt).isNotNull()
  }

  @Test
  fun `an organization cannot enable an account whose disable origin is unknown`() {
    val data = saveTestData()
    assertThrows<ValidationException> {
      userAccountService.enable(data.nullOriginDisabledManagedMember.id, UserDisabledBy.ORGANIZATION)
    }
    val user = userAccountService.findActiveOrDisabled(data.nullOriginDisabledManagedMember.id)!!
    assertThat(user.disabledAt).isNotNull()
    assertThat(user.disabledBy).isNull()
  }

  @Test
  fun `an organization disable is rejected on an account whose disable origin is unknown`() {
    val data = saveTestData()
    val events =
      captureUserCountEvents {
        assertThrows<ValidationException> {
          userAccountService.disable(data.nullOriginDisabledManagedMember.id, UserDisabledBy.ORGANIZATION)
        }
      }
    assertThat(events).isEmpty()
    assertThat(userAccountService.findActiveOrDisabled(data.nullOriginDisabledManagedMember.id)!!.disabledBy)
      .isNull()
  }

  @Test
  fun `an admin can enable an account whose disable origin is unknown`() {
    val data = saveTestData()
    userAccountService.enable(data.nullOriginDisabledManagedMember.id, UserDisabledBy.ADMIN)
    assertThat(userAccountService.findActiveOrDisabled(data.nullOriginDisabledManagedMember.id)!!.disabledAt)
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
