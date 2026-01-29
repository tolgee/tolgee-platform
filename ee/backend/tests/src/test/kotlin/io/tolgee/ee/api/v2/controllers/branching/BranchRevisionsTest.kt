package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BranchRevisionData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull

@SpringBootTest
@AutoConfigureMockMvc
class BranchRevisionsTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BranchRevisionData

  @Autowired
  private lateinit var branchRepository: BranchRepository

  @BeforeEach
  fun setup() {
    testData = BranchRevisionData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
    currentDateProvider.forcedDate = currentDateProvider.date
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `editing key increases branch revision`() {
    keyService.edit(testData.firstKey.id, EditKeyDto(name = "first_key", description = "test description"))
    assertBranchMetadataChanged()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `editing translation text increases branch revision`() {
    translationService.setForKey(
      testData.firstKey,
      mapOf("en" to "new translation text"),
    )
    assertBranchMetadataChanged()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `changing translation state increases branch revision`() {
    translationService.setStateBatch(testData.translation, TranslationState.REVIEWED)
    assertBranchMetadataChanged()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deleting key increases branch revision`() {
    keyService.delete(testData.firstKey.id)
    assertBranchMetadataChanged()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deleting translation increases branch revision`() {
    translationService.deleteByIdIn(listOf(testData.translation.id))
    assertBranchMetadataChanged()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `adding key increases branch revision`() {
    keyService.create(project, CreateKeyDto(name = "new_key", description = "test description", branch = "dev"))
    assertBranchMetadataChanged()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `adding key tag increases branch revision`() {
    tagService.tagKey(testData.project.id, testData.firstKey.id, "test")
    assertBranchMetadataChanged()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `removing key tag increases branch revision`() {
    tagService.removeTag(testData.project.id, testData.firstKey.id, testData.tag.id)
    assertBranchMetadataChanged()
  }

  private fun assertBranchMetadataChanged() {
    waitForNotThrowing(timeout = 3000, pollTime = 500) {
      testData.devBranch
        .refresh()
        .revision.assert
        .isGreaterThan(0)
    }
  }

  private fun Branch.refresh(): Branch = branchRepository.findByIdOrNull(this.id)!!
}
