package io.tolgee.service.key

import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.service.language.LanguageService
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KeyTrashService(
  private val languageService: LanguageService,
  private val translationViewDataProvider: TranslationViewDataProvider,
  private val screenshotService: ScreenshotService,
) {
  @Transactional
  fun getTrashedKeysWithTranslations(
    projectId: Long,
    userId: Long,
    pageable: Pageable,
    params: TranslationFilters,
  ): Page<KeyWithTranslationsView> {
    params.trashed = true
    val languages =
      languageService.getLanguagesForTranslationsView(
        params.languages,
        projectId,
        userId,
      )
    val data =
      translationViewDataProvider.getData(
        projectId = projectId,
        languages = languages.toSet(),
        pageable = pageable,
        params = params,
      )
    val keyIds = data.content.map { it.keyId }
    if (keyIds.isNotEmpty()) {
      val screenshotsByKeyId = screenshotService.getScreenshotsForKeys(keyIds)
      data.content.forEach { it.screenshots = screenshotsByKeyId[it.keyId] ?: emptyList() }
    }
    return data
  }
}
