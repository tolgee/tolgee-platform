package io.tolgee.model.views

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.queryResults.ProjectStatistics

class ProjectWithStatsView(
  view: ProjectWithLanguagesView,
  val stats: ProjectStatistics,
  val languages: List<LanguageDto>,
) : ProjectWithLanguagesView(
    view.id,
    view.name,
    view.description,
    view.slug,
    view.avatarHash,
    view.useNamespaces,
    view.defaultNamespace,
    view.organizationOwner,
    view.organizationRole,
    view.directPermission,
    view.icuPlaceholders,
    view.suggestionsMode,
    view.translationProtection,
    view.permittedLanguageIds,
  )
