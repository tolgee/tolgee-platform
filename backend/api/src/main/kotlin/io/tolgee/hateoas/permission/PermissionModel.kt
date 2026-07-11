package io.tolgee.hateoas.permission

import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.springframework.hateoas.RepresentationModel

open class PermissionModel(
  override val scopes: Array<Scope>,
  override val type: ProjectPermissionType?,
  override val permittedLanguageIds: Collection<Long>?,
  override val translateLanguageIds: Collection<Long>?,
  override val viewLanguageIds: Collection<Long>?,
  override val stateChangeLanguageIds: Collection<Long>?,
  override val suggestLanguageIds: Collection<Long>?,
) : RepresentationModel<PermissionModel>(),
  IDeprecatedPermissionModel
