package io.tolgee.api.v2.hateoas.permission

import io.tolgee.constants.ComputedPermissionOrigin
import org.springframework.hateoas.RepresentationModel

open class ComputedPermissionModel(
  permissionModel: PermissionModel,
  val origin: ComputedPermissionOrigin
) : RepresentationModel<ComputedPermissionModel>(), IPermissionModel by permissionModel
