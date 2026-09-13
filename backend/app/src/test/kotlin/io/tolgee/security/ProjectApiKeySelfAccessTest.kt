package io.tolgee.security

import io.tolgee.development.testDataBuilder.data.ProjectApiKeySelfAccessTestData
import io.tolgee.dtos.request.translation.comment.TranslationCommentDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

/**
 * The author-self elevation ("you may act on what you created") is a user-authority path. A project API key is a scoped
 * credential, so it must carry the real scope even when the underlying user authored the thing it is acting on —
 * otherwise the elevation lets the key act past the scope list it was issued with.
 */
@AutoConfigureMockMvc
class ProjectApiKeySelfAccessTest : AbstractControllerTest() {
  private lateinit var testData: ProjectApiKeySelfAccessTestData

  @BeforeEach
  fun setup() {
    testData = ProjectApiKeySelfAccessTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `the own-comment fallback cannot widen a key that lacks comments-edit`() {
    val commentUrl =
      "/v2/projects/${testData.project.id}/translations/${testData.ownCommentTranslation.id}/comments/${testData.ownComment.id}"

    performPut("$commentUrl?ak=${key(Scope.TRANSLATIONS_VIEW)}", TranslationCommentDto(text = "edited")).andIsForbidden
    performDelete("$commentUrl?ak=${key(Scope.TRANSLATIONS_VIEW)}", null).andIsForbidden

    val editing = key(Scope.TRANSLATIONS_VIEW, Scope.TRANSLATIONS_COMMENTS_EDIT)
    performPut("$commentUrl?ak=$editing", TranslationCommentDto(text = "edited")).andIsOk
    performDelete("$commentUrl?ak=$editing", null).andIsOk
  }

  @Test
  fun `the own-batch-job fallback cannot widen a key that lacks batch-jobs scopes`() {
    val jobUrl = "/v2/projects/${testData.project.id}/batch-jobs/${testData.ownBatchJob.id}"

    performGet("$jobUrl?ak=${key(Scope.TRANSLATIONS_VIEW)}").andIsForbidden
    performPut("$jobUrl/cancel?ak=${key(Scope.TRANSLATIONS_VIEW)}", null).andIsForbidden

    performGet("$jobUrl?ak=${key(Scope.TRANSLATIONS_VIEW, Scope.BATCH_JOBS_VIEW)}").andIsOk
  }

  @Test
  fun `the own-jobs listing fallback cannot widen a key that lacks batch-jobs-view`() {
    val currentJobs = "/v2/projects/${testData.project.id}/current-batch-jobs"

    performGet("$currentJobs?ak=${key(Scope.TRANSLATIONS_VIEW)}").andIsForbidden
    performGet("$currentJobs?ak=${key(Scope.TRANSLATIONS_VIEW, Scope.BATCH_JOBS_VIEW)}").andIsOk
  }

  private fun key(vararg scopes: Scope): String =
    apiKeyService.create(testData.user, scopes.toSet(), testData.projectBuilder.self).key!!
}
