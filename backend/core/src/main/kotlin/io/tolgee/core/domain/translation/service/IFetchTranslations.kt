package io.tolgee.core.domain.translation.service

import io.tolgee.core.concepts.types.FailureMarker
import io.tolgee.core.concepts.types.OutputMarker
import io.tolgee.core.domain.project.data.ProjectId
import io.tolgee.core.domain.translation.data.TranslationId
import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

interface IFetchTranslations {
  sealed interface Output : OutputMarker {
    data class Success(val translation: Translation) : Output

    data object NotFound : Output, FailureMarker
  }

  fun byId(
    projectId: ProjectId,
    translationId: TranslationId,
  ): Output
}

@Service
@Transactional(propagation = Propagation.MANDATORY)
class IFetchTranslationsImpl(
  private val translationQueries: INameTranslationQueries,
) : IFetchTranslations {
  override fun byId(
    projectId: ProjectId,
    translationId: TranslationId,
  ): IFetchTranslations.Output {
    val translation = translationQueries.find(projectId.value, translationId.value)
      ?: return IFetchTranslations.Output.NotFound

    return IFetchTranslations.Output.Success(translation)
  }
}
