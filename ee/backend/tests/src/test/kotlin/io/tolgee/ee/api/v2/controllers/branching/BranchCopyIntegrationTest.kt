package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BranchTranslationsTestData
import io.tolgee.ee.repository.BranchRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.key.Key
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.servlet.ResultActions
import kotlin.system.measureTimeMillis

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class BranchCopyIntegrationTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: BranchTranslationsTestData

  @Autowired
  lateinit var branchRepository: BranchRepository

  @BeforeEach
  fun setup() {
    testData = BranchTranslationsTestData()
    projectSupplier = { testData.project }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `copies keys and translations to new branch`() {
    val projectId = testData.project.id

    performBranchCreation().andIsOk.andAssertThatJson {
      node("name").isEqualTo("feature-x")
      node("active").isEqualTo(true)
    }

    val newBranchId = branchRepository.findByProjectIdAndName(projectId, "feature-x")!!.id
    val newBranchKeyCount = keyRepository.countByProjectAndBranch(projectId, newBranchId)

    newBranchKeyCount.assert.isEqualTo(500)

    val firstMainKey = keyRepository.findPrefetchedByNameAndBranch(projectId, "branched key 1", "main")
    val firstBranchKey = keyRepository.findPrefetchedByNameAndBranch(projectId, "branched key 1", "feature-x")

    firstBranchKey!!.branch!!.id.assert.isEqualTo(newBranchId)
    firstBranchKey.assertIsCopyOf(firstMainKey!!)

    // branch should be ready
    val branch = branchRepository.findByIdOrNull(newBranchId)!!
    branch.pending.assert.isFalse()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `copying a lot data is not slow`() {
    val data = testData.generateBunchData(2000)
    testDataService.saveTestData { data.build {} }
    var response: ResultActions
    val time = measureTimeMillis {
      response = performBranchCreation()
    }
    response.andIsOk
    time.assert.isLessThan(3000)
  }

  private fun performBranchCreation(): ResultActions {
    return performProjectAuthPost(
      "branches",
      mapOf(
        "name" to "feature-x",
        "originBranchId" to testData.mainBranch.id,
      )
    )
  }

  private fun Key.assertIsCopyOf(other: Key) {
    val mainTranslations = this.translations
    val branchTranslations = other.translations
    val mainTranslation = mainTranslations.find { t -> t.language.tag == "en" }!!
    val branchTranslation = branchTranslations.find { t -> t.language.tag == "en" }!!
    val mainTranslationLabels = mainTranslation.labels
    val branchTranslationLabels = branchTranslation.labels

    // only key.id is different
    this.id.assert.isNotEqualTo(other.id)
    this.namespace.assert.isEqualTo(other.namespace)
    this.name.assert.isEqualTo(other.name)
    this.isPlural.assert.isEqualTo(other.isPlural)
    this.pluralArgName.assert.isEqualTo(other.pluralArgName)

    // translations are the same
    mainTranslations.assert.hasSize(branchTranslations.size)
    mainTranslation.assert.isNotNull()
    branchTranslation.assert.isNotNull()
    mainTranslation.text.assert.isEqualTo(branchTranslation.text)
    mainTranslation.state.assert.isEqualTo(branchTranslation.state)
    mainTranslation.auto.assert.isEqualTo(branchTranslation.auto)
    mainTranslation.mtProvider.assert.isEqualTo(branchTranslation.mtProvider)
    mainTranslation.outdated.assert.isEqualTo(branchTranslation.outdated)
    mainTranslation.wordCount.assert.isEqualTo(branchTranslation.wordCount)
    mainTranslation.characterCount.assert.isEqualTo(branchTranslation.characterCount)

    // labels are the same
    mainTranslationLabels.assert.hasSize(branchTranslationLabels.size)
    mainTranslationLabels.assert.containsAll(branchTranslationLabels)
  }
}
