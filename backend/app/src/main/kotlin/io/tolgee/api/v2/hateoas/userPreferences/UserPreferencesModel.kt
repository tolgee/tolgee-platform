package io.tolgee.api.v2.hateoas.userPreferences

import org.springframework.hateoas.RepresentationModel

class UserPreferencesModel(
  var language: String?,
  var preferredOrganizationId: Long?
) : RepresentationModel<UserPreferencesModel>()
