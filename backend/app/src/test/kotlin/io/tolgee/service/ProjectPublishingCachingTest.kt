/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class ProjectPublishingCachingTest : AbstractSpringTest() {
  private lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    clearCaches()
  }

  @Test
  fun `it evicts the project cache when publishing state changes`() {
    populateCache()
    executeInNewTransaction {
      projectService.setPublic(testData.project.id, true)
    }
    assertCacheEvicted()
  }

  private fun populateCache() {
    projectService.findDto(testData.project.id)
    cacheManager
      .getCache(Caches.PROJECTS)!!
      .get(testData.project.id)
      ?.get()
      .assert
      .isNotNull
  }

  private fun assertCacheEvicted() {
    cacheManager
      .getCache(Caches.PROJECTS)!!
      .get(testData.project.id)
      ?.get()
      .assert
      .isNull()
  }
}
