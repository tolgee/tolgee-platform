package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class KeySoftDeleteTaskTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TaskTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `soft-deleted key is excluded from task`() {
    val taskNumber = testData.translateTask.self.number
    val keyToDelete = testData.keysInTask.first().self

    // Before soft-delete, key should be in task
    performProjectAuthGet("tasks/$taskNumber/keys").andIsOk.andAssertThatJson {
      node("keys").isArray.contains(keyToDelete.id)
    }

    // Soft-delete the key
    performProjectAuthDelete("keys/${keyToDelete.id}", null).andIsOk

    // After soft-delete, key should NOT be in task
    performProjectAuthGet("tasks/$taskNumber/keys").andIsOk.andAssertThatJson {
      node("keys").isArray.doesNotContain(keyToDelete.id)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `restored key is visible in task again`() {
    val taskNumber = testData.translateTask.self.number
    val keyToDelete = testData.keysInTask.first().self

    // Soft-delete the key
    performProjectAuthDelete("keys/${keyToDelete.id}", null).andIsOk

    // Verify key is not in task
    performProjectAuthGet("tasks/$taskNumber/keys").andIsOk.andAssertThatJson {
      node("keys").isArray.doesNotContain(keyToDelete.id)
    }

    // Restore the key
    performProjectAuthPut("keys/trash/${keyToDelete.id}/restore", null).andIsOk

    // Key should be back in task
    performProjectAuthGet("tasks/$taskNumber/keys").andIsOk.andAssertThatJson {
      node("keys").isArray.contains(keyToDelete.id)
    }
  }
}
