package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BranchRevisionData
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.ee.repository.BranchRepository
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
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `editing key increases branch revision`() {
    keyService.edit(testData.firstKey.id, EditKeyDto(name = "first_key", description = "test description"))
    testData.devBranch.refresh().revision.assert.isEqualTo(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `editing translation text increases branch revision`() {
    translationService.setForKey(
      testData.firstKey,
      mapOf("en" to "new translation text")
    )
    testData.devBranch.refresh().revision.assert.isEqualTo(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `changing translation state increases branch revision`() {
    translationService.setStateBatch(testData.translation, TranslationState.REVIEWED)
    testData.devBranch.refresh().revision.assert.isEqualTo(1)
  }

  private fun Branch.refresh(): Branch = branchRepository.findByIdOrNull(this.id)!!
}
