package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.service.project.DeletedOrganizationProjectPurgeScheduler
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DeletedOrganizationProjectPurgeSchedulerTest : AbstractSpringTest() {
  @Autowired
  lateinit var purgeScheduler: DeletedOrganizationProjectPurgeScheduler

  @Test
  fun `purges projects of a soft-deleted organization`() {
    val testData = BaseTestData()
    executeInNewTransaction {
      testDataService.saveTestData(testData.root)
    }
    val organizationId = testData.projectBuilder.self.organizationOwner.id
    val projectId = testData.projectBuilder.self.id

    executeInNewTransaction {
      organizationService.delete(organizationService.get(organizationId))
    }

    purgeScheduler.purge()

    executeInNewTransaction {
      projectService.findIncludingDeleted(projectId).assert.isNull()
    }
  }

  @Test
  fun `keeps projects of an active organization`() {
    val testData = BaseTestData()
    executeInNewTransaction {
      testDataService.saveTestData(testData.root)
    }
    val projectId = testData.projectBuilder.self.id

    purgeScheduler.purge()

    executeInNewTransaction {
      projectService.findIncludingDeleted(projectId).assert.isNotNull
    }
  }
}
