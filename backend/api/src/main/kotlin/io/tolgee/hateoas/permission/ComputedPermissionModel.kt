package io.tolgee.hateoas.permission

import io.tolgee.constants.ComputedPermissionOrigin
import org.springframework.hateoas.RepresentationModel

open class ComputedPermissionModel(
  permissionModel: PermissionModel,
  val origin: ComputedPermissionOrigin,
) : RepresentationModel<ComputedPermissionModel>(),
  IDeprecatedPermissionModel by permissionModel
