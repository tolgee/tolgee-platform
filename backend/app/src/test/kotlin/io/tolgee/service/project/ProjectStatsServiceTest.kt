/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.project

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.ProjectsTestData
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class ProjectStatsServiceTest : AbstractSpringTest() {

  @Autowired
  lateinit var projectStatsService: ProjectStatsService

  @Test
  fun `test get statistics`() {
    val projectTestData = ProjectsTestData()
    testDataService.saveTestData(projectTestData.root)
    val result = projectStatsService.getProjectsTotals(
      listOf(
        projectTestData.projectBuilder.self.id,
        projectTestData.project2.id
      )
    )
    result[0]
  }
}
