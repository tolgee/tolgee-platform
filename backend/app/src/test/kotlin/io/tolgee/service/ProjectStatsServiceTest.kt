package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.ProjectStatsTestData
import io.tolgee.service.project.ProjectStatsService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ProjectStatsServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var projectStatsService: ProjectStatsService

  lateinit var testData: ProjectStatsTestData

  @BeforeEach
  fun setup() {
    testData = ProjectStatsTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `test project stats`() {
    val data = projectStatsService.getProjectStats(testData.projectBuilder.self.id)
    assertThat(data.id).isPositive
    assertThat(data.memberCount).isEqualTo(3)
    assertThat(data.keyCount).isEqualTo(5)
    assertThat(data.tagCount).isEqualTo(3)
  }

  @Test
  fun `test project stats on feature branch`() {
    val data = projectStatsService.getProjectStats(testData.projectBuilder.self.id, testData.featureBranch.id)
    assertThat(data.id).isPositive
    assertThat(data.memberCount).isEqualTo(3)
    assertThat(data.keyCount).isEqualTo(1)
    assertThat(data.tagCount).isEqualTo(1)
  }
}
