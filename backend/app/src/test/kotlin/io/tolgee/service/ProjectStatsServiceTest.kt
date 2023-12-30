package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.ProjectStatsTestData
import io.tolgee.service.project.ProjectStatsService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ProjectStatsServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var projectStatsService: ProjectStatsService

  @Test
  fun getProjectStats() {
    val testData = ProjectStatsTestData()
    testDataService.saveTestData(testData.root)
    val data = projectStatsService.getProjectStats(testData.projectBuilder.self.id)
    assertThat(data.id).isPositive
    assertThat(data.memberCount).isEqualTo(3)
    assertThat(data.keyCount).isEqualTo(5)
    assertThat(data.tagCount).isEqualTo(3)
  }
}
