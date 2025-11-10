package io.tolgee.hateoas.permission

import io.tolgee.hateoas.TranslationAgencySimpleModel
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.springframework.hateoas.RepresentationModel

class PermissionWithAgencyModel(
  override val scopes: Array<Scope>,
  override val type: ProjectPermissionType?,
  override val permittedLanguageIds: Collection<Long>?,
  override val translateLanguageIds: Collection<Long>?,
  override val viewLanguageIds: Collection<Long>?,
  override val stateChangeLanguageIds: Collection<Long>?,
  override val suggestLanguageIds: Collection<Long>?,
  val agency: TranslationAgencySimpleModel?,
) : RepresentationModel<PermissionModel>(),
  IDeprecatedPermissionModel
