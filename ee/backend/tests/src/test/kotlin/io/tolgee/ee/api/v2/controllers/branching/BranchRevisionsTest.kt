package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BranchRevisionData
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
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

  @Autowired
  private lateinit var keyRepository: KeyRepository

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
      mapOf("en" to "new translation text")
    )
    assertBranchMetadataChanged()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `changing translation state increases branch revision`() {
    translationService.setStateBatch(testData.translation, TranslationState.REVIEWED)
    assertBranchMetadataChanged()
  }

  private fun assertBranchMetadataChanged() {
    waitForNotThrowing(timeout = 3000, pollTime = 500) {
      testData.devBranch.refresh().revision.assert.isEqualTo(1)
    }
    testData.firstKey.refresh().let {
      it.cascadeUpdatedAt.assert.isNotNull
      it.cascadeUpdatedAt!!.time.assert.isEqualTo(currentDateProvider.date.time)
    }
  }

  private fun Branch.refresh(): Branch = branchRepository.findByIdOrNull(this.id)!!
  private fun Key.refresh(): Key = keyRepository.findByIdOrNull(this.id)!!
}
