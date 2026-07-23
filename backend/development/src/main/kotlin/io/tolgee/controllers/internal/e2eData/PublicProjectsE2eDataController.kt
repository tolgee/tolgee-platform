package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.data.StandardTestDataResult
import io.tolgee.data.service.TestDataGeneratingService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.PublicProjectsE2eData
import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/public-projects"])
class PublicProjectsE2eDataController(
  private val generatingService: TestDataGeneratingService,
) : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  private lateinit var currentTestData: PublicProjectsE2eData

  override val testData: TestDataBuilder
    get() {
      currentTestData = PublicProjectsE2eData()
      return currentTestData.root
    }

  override fun afterTestDataStored(data: TestDataBuilder) {
    val project = currentTestData.outsiderProject ?: return
    entityManager.persist(
      ActivityRevision().apply {
        projectId = project.id
        authorId = currentTestData.contributingUser.id
      },
    )
    entityManager.flush()
  }

  @GetMapping(value = ["/generate-few"])
  @Transactional
  fun generateFew(): StandardTestDataResult {
    return generatingService.generate(PublicProjectsE2eData(count = 5, includeForeignOrgProject = false).root)
  }
}
