package io.tolgee.core.domain.translation.comment.service

import io.tolgee.core.concepts.types.FailureMarker
import io.tolgee.core.concepts.types.OutputMarker
import io.tolgee.core.domain.project.data.ProjectId
import io.tolgee.core.domain.translation.data.TranslationId
import io.tolgee.core.domain.translation.service.IFetchTranslations
import io.tolgee.model.translation.TranslationComment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

interface IFetchTranslationComments {
  sealed interface Output : OutputMarker {
    data class Success(val page: Page<TranslationComment>) : Output

    data object TranslationNotFound : Output, FailureMarker
  }

  fun forTranslation(
    projectId: ProjectId,
    translationId: TranslationId,
    pageable: Pageable,
  ): Output
}

@Repository
@Transactional(propagation = Propagation.MANDATORY)
class IFetchTranslationCommentsImpl(
  private val fetchTranslations: IFetchTranslations,
  private val commentQueries: INameTranslationCommentQueries,
) : IFetchTranslationComments {
  override fun forTranslation(
    projectId: ProjectId,
    translationId: TranslationId,
    pageable: Pageable,
  ): IFetchTranslationComments.Output {
    // Verify translation exists and belongs to project
    when (fetchTranslations.byId(projectId, translationId)) {
      is IFetchTranslations.Output.NotFound -> return IFetchTranslationComments.Output.TranslationNotFound
      is IFetchTranslations.Output.Success -> Unit
    }

    val page = commentQueries.getPagedByProjectAndTranslationId(projectId.value, translationId.value, pageable)
    return IFetchTranslationComments.Output.Success(page)
  }
}
