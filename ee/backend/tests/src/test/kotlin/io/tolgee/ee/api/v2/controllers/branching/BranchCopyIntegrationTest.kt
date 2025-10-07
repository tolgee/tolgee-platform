package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BranchTranslationsTestData
import io.tolgee.ee.repository.BranchRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyMetaRepository
import io.tolgee.repository.KeyScreenshotReferenceRepository
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.ResultActions
import kotlin.system.measureTimeMillis

@ActiveProfiles(profiles = ["test"])
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class BranchCopyIntegrationTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: BranchTranslationsTestData

  @Autowired
  lateinit var branchRepository: BranchRepository

  @Autowired
  lateinit var keyMetaRepository: KeyMetaRepository

  @Autowired
  lateinit var keyScreenshotReferenceRepository: KeyScreenshotReferenceRepository

  @BeforeEach
  fun setup() {
    testData = BranchTranslationsTestData()
    projectSupplier = { testData.project }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `copies keys and translations to new branch`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    val projectId = testData.project.id

    performBranchCreation().andIsOk.andAssertThatJson {
      node("name").isEqualTo("feature-x")
      node("active").isEqualTo(true)
    }

    val newBranchId = branchRepository.findByProjectIdAndName(projectId, "feature-x")!!.id
    val newBranchKeyCount = keyRepository.countByProjectAndBranch(projectId, newBranchId)

    newBranchKeyCount.assert.isEqualTo(50)

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
    testData.generateBunchData(2000)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    var response: ResultActions
    val time = measureTimeMillis {
      response = performBranchCreation()
    }
    response.andIsOk
    time.assert.isLessThan(3000)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `delete branch and its related data`() {
    testData.addBranchToBeDeleted()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    performBranchDeletion(testData.toBeDeletedBranch.id).andIsOk

    waitForNotThrowing(timeout = 10000, pollTime = 250) {
      keyRepository.countByProjectAndBranch(
        testData.project.id,
        testData.toBeDeletedBranch.id
      ).assert.isEqualTo(0)
    }
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

  private fun performBranchDeletion(id: Long): ResultActions {
    return performProjectAuthDelete("branches/$id")
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

    // tags are the same
    this.keyMeta!!.tags.assert.hasSize(other.keyMeta!!.tags.size)
    this.keyMeta!!.tags.assert.containsAll(other.keyMeta!!.tags)

    // translation comments are the same
    mainTranslation.comments.assert.hasSize(branchTranslation.comments.size)
    mainTranslation.comments.first().let { comment ->
      val branchComment = branchTranslation.comments.first()
      comment.id.assert.isNotEqualTo(branchComment.id)
      comment.text.assert.isEqualTo(branchComment.text)
      comment.state.assert.isEqualTo(branchComment.state)
      comment.author.assert.isEqualTo(branchComment.author)
    }

    // key screenshots are the same
    val mainKeyScreenshots = keyScreenshotReferenceRepository.getAllByKeyIdIn(listOf(this.id))
    val branchKeyScreenshots = keyScreenshotReferenceRepository.getAllByKeyIdIn(listOf(other.id))
    mainKeyScreenshots.first().let { screenshotReference ->
      val branchScreenshotReference = branchKeyScreenshots.first()
      screenshotReference.screenshot.assert.isEqualTo(branchScreenshotReference.screenshot)
      screenshotReference.originalText.assert.isEqualTo(branchScreenshotReference.originalText)
      screenshotReference.positions.assert.isEqualTo(branchScreenshotReference.positions)
    }

    val mainKeyMetaCodeReferences = keyMetaRepository.getCodeReferencesByKeyMetaId(this.keyMeta!!.id)
    val branchKeyMetaCodeReferences = keyMetaRepository.getCodeReferencesByKeyMetaId(other.keyMeta!!.id)

    mainKeyMetaCodeReferences.assert.hasSizeGreaterThan(0)
    mainKeyMetaCodeReferences.assert.hasSize(branchKeyMetaCodeReferences.size)
    mainKeyMetaCodeReferences.first().let { codeReference ->
      val branchCodeReference = branchKeyMetaCodeReferences.first()
      codeReference.id.assert.isNotEqualTo(branchCodeReference.id)
      codeReference.line.assert.isEqualTo(branchCodeReference.line)
      codeReference.path.assert.isEqualTo(branchCodeReference.path)
      codeReference.author.assert.isEqualTo(branchCodeReference.author)
      codeReference.fromImport.assert.isEqualTo(branchCodeReference.fromImport)
    }
  }
}
