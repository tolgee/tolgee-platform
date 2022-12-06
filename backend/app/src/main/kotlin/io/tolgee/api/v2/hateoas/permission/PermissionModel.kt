package io.tolgee.api.v2.hateoas.permission

import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.springframework.hateoas.RepresentationModel

open class PermissionModel(
  override val scopes: Array<Scope>,
  override val type: ProjectPermissionType?,
  override val permittedLanguageIds: Collection<Long>?,
  override val granular: Boolean
) : RepresentationModel<PermissionModel>(), IPermissionModel
