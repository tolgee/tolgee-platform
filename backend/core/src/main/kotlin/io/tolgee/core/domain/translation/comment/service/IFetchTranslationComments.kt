package io.tolgee.core.domain.translation.comment.service

import io.tolgee.core.concepts.conversions.Into
import io.tolgee.core.concepts.conversions.converting
import io.tolgee.core.concepts.conversions.shortCircuit
import io.tolgee.core.concepts.types.FailureMarker
import io.tolgee.core.concepts.types.OutputMarker
import io.tolgee.core.domain.project.data.ProjectId
import io.tolgee.core.domain.translation.comment.service.IFetchTranslationComments.IntoOutput.bind
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

  object IntoOutput : Into<Output> {
    fun IFetchTranslations.Output.bind() = when (this) {
      is IFetchTranslations.Output.NotFound -> shortCircuit(Output.TranslationNotFound)
      is IFetchTranslations.Output.Success -> Unit
    }
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
  ): IFetchTranslationComments.Output = converting(IFetchTranslationComments.IntoOutput) {
    fetchTranslations.byId(projectId, translationId).bind()
    val page = commentQueries.getPagedByProjectAndTranslationId(projectId.value, translationId.value, pageable)
    IFetchTranslationComments.Output.Success(page)
  }
}
