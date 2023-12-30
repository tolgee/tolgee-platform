package io.tolgee.model.views

import io.tolgee.dtos.queryResults.ProjectStatistics

class ProjectWithStatsView(
  view: ProjectWithLanguagesView,
  val stats: ProjectStatistics,
  val languages: List<LanguageView>,
) : ProjectWithLanguagesView(
    view.id,
    view.name,
    view.description,
    view.slug,
    view.avatarHash,
    view.baseLanguage,
    view.organizationOwner,
    view.organizationRole,
    view.directPermission,
    view.permittedLanguageIds,
  )
