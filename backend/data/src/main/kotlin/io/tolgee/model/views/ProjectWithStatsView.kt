package io.tolgee.model.views

import io.tolgee.dtos.query_results.ProjectStatistics
import io.tolgee.model.Language

class ProjectWithStatsView(
  view: ProjectWithLanguagesView,
  val stats: ProjectStatistics,
  val languages: List<Language>,
) : ProjectWithLanguagesView(
  view.id,
  view.name,
  view.description,
  view.slug,
  view.avatarHash,
  view.userOwner,
  view.baseLanguage,
  view.organizationOwnerName,
  view.organizationOwnerSlug,
  view.organizationBasePermissions,
  view.organizationRole,
  view.directPermissions,
  view.permittedLanguageIds
)
