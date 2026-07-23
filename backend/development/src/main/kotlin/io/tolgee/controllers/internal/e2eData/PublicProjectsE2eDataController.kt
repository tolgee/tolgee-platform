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

  private lateinit var data: PublicProjectsE2eData

  // Each access builds a fresh graph (save assigns ids) and captures the instance so
  // afterTestDataStored can read the just-persisted "Community Outsider" project to seed a contribution.
  override val testData: TestDataBuilder
    get() {
      data = PublicProjectsE2eData()
      return data.root
    }

  // The community page reads contributions, not memberships. publicProjectsUser owns the "Community *"
  // projects (a member of those), so it only qualifies as a contributor on the foreign-org project it
  // is not a member of — seed one activity revision there so the mine-only view and the switcher entry
  // have something to show.
  override fun afterTestDataStored(data: TestDataBuilder) {
    val project = this.data.outsiderProject ?: return
    entityManager.persist(
      ActivityRevision().apply {
        projectId = project.id
        authorId = this@PublicProjectsE2eDataController.data.contributingUser.id
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
