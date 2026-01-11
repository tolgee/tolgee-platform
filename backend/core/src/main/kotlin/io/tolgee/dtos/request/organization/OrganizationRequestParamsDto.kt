package io.tolgee.dtos.request.organization

data class OrganizationRequestParamsDto(
  val filterCurrentUserOwner: Boolean = false,
  val search: String? = "",
)
