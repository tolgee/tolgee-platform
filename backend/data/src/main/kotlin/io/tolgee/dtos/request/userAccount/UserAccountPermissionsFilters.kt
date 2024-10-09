package io.tolgee.dtos.request.userAccount

import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

open class UserAccountPermissionsFilters {
  @field:Parameter(
    description = """Filter users by id""",
  )
  var filterId: List<Long>? = null

  @field:Parameter(
    description = """Filter only users that have at least following scopes""",
  )
  var filterMinimalScope: String? = null

  @field:Parameter(
    description = """Filter only users that can view language""",
  )
  var filterViewLanguageId: Long? = null

  @field:Parameter(
    description = """Filter only users that can edit language""",
  )
  var filterEditLanguageId: Long? = null

  @field:Parameter(
    description = """Filter only users that can edit state of language""",
  )
  var filterStateLanguageId: Long? = null

  val filterMinimalScopeExtended get(): String? {
    return filterMinimalScope?.let {
      "{${Scope.selfAndAncestors(Scope.valueOf(it)).joinToString(",")}}"
    }
  }

  val filterMinimalPermissionType get(): List<String> {
    return filterMinimalScope?.let {
      ProjectPermissionType.findByScope(Scope.valueOf(it)).map { it.toString() }
    } ?: listOf()
  }
}
