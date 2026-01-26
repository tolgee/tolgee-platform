package io.tolgee.core.scenario.translation

import io.tolgee.core.concepts.architecture.Scenario
import io.tolgee.core.concepts.architecture.ScenarioService
import io.tolgee.core.concepts.security.AuthorizationWitness
import io.tolgee.core.concepts.types.FailureMarker
import io.tolgee.core.concepts.types.InputMarker
import io.tolgee.core.concepts.types.OutputMarker
import io.tolgee.core.domain.project.data.ProjectId
import io.tolgee.core.domain.project.service.ICheckProjectAuthorization
import io.tolgee.core.domain.project.service.IFetchProjects
import io.tolgee.core.domain.translation.comment.service.IFetchTranslationComments
import io.tolgee.core.domain.translation.data.TranslationId
import io.tolgee.core.domain.user.data.UserId
import io.tolgee.core.scenario.translation.FetchTranslationComments.Input
import io.tolgee.core.scenario.translation.FetchTranslationComments.Output
import io.tolgee.core.scenario.translation.FetchTranslationComments.Witness
import io.tolgee.model.translation.TranslationComment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@ScenarioService
class FetchTranslationComments(
  private val fetchProjects: IFetchProjects,
  private val fetchComments: IFetchTranslationComments,
  private val checkProjectAuthorization: ICheckProjectAuthorization,
) : Scenario<Input, Output, Witness> {

  data class Input(
    val projectId: ProjectId,
    val translationId: TranslationId,
    val pageable: Pageable,
  ) : InputMarker

  sealed interface Output : OutputMarker {
    data class Success(val page: Page<TranslationComment>) : Output

    /**
     * Project doesn't exist in the database.
     */
    data object ProjectNotFound : Output, FailureMarker

    /**
     * Project exists but user has no permission to access it.
     */
    data object ProjectNotAccessible : Output, FailureMarker

    /**
     * Translation doesn't exist or belongs to a different project.
     */
    data object TranslationNotFound : Output, FailureMarker

    /**
     * Rate limit exceeded.
     *
     * @param retryAfterMs Milliseconds until the rate limit resets
     */
    data class RateLimited(val retryAfterMs: Long) : Output, FailureMarker
  }

  class Witness internal constructor() : AuthorizationWitness

  /**
   * Authorize access to the specified project for the given user.
   *
   * @param userId The user requesting access
   * @param projectId The project to access
   * @return [Witness] if user has access, `null` if access is denied
   */
  fun authorize(userId: UserId, projectId: ProjectId): Witness? {
    return when (checkProjectAuthorization.hasAccess(userId, projectId)) {
      is ICheckProjectAuthorization.Output.Authorized -> Witness()
      is ICheckProjectAuthorization.Output.Denied -> null
    }
  }

  @Transactional
  override fun Witness.execute(input: Input): Output {
    // Check project exists
    when (fetchProjects.byId(input.projectId)) {
      is IFetchProjects.Output.NotFound -> return Output.ProjectNotFound
      is IFetchProjects.Output.Success -> Unit
    }

    // Fetch comments (includes translation existence check)
    return when (val result = fetchComments.forTranslation(input.projectId, input.translationId, input.pageable)) {
      is IFetchTranslationComments.Output.TranslationNotFound -> Output.TranslationNotFound
      is IFetchTranslationComments.Output.Success -> Output.Success(result.page)
    }
  }
}
