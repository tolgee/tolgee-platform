package io.tolgee.dtos.request.project

class SetPermissionLanguageParams(
  override var languages: Set<Long>? = null,
  override var translateLanguages: Set<Long>? = null,
  override var viewLanguages: Set<Long>? = null,
  override var stateChangeLanguages: Set<Long>? = null,
  override var suggestLanguages: Set<Long>? = null,
) : RequestWithLanguagePermissions
