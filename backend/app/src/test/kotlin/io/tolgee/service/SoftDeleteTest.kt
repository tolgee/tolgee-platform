package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable

class SoftDeleteTest : AbstractSpringTest() {
  @Test
  fun `project is soft deleted`() {
    val testData = BaseTestData()
    executeInNewTransaction {
      testDataService.saveTestData(testData.root)
    }
    executeInNewTransaction {
      projectService.deleteProject(testData.projectBuilder.self.id)
    }
    val result =
      entityManager
        .createNativeQuery("select deleted_at from project where id = :id")
        .setParameter("id", testData.projectBuilder.self.id)
        .singleResult

    result.assert.isNotNull
  }

  @Test
  fun `queries don't return deleted projects`() {
    val testData = BaseTestData()
    executeInNewTransaction {
      testDataService.saveTestData(testData.root)
    }

    executeInNewTransaction {
      projectService.deleteProject(testData.projectBuilder.self.id)
    }

    executeInNewTransaction {
      projectService.findAllPermitted(testData.user).assert.isEmpty()
      projectService
        .findPermittedInOrganizationPaged(
          pageable = Pageable.ofSize(100),
          search = null,
          organizationId = testData.projectBuilder.self.organizationOwner.id,
          userAccountId = testData.user.id,
        ).assert
        .isEmpty()
    }
  }
}
