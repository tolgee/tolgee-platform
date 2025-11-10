package io.tolgee.dtos.cacheable

import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import java.io.Serializable

data class PermissionDto(
  val id: Long,
  val userId: Long?,
  val invitationId: Long?,
  override val scopes: Array<Scope>,
  override val projectId: Long?,
  override val organizationId: Long?,
  override val translateLanguageIds: Set<Long>? = null,
  override val type: ProjectPermissionType?,
  override val granular: Boolean?,
  override val viewLanguageIds: Set<Long>?,
  override val stateChangeLanguageIds: Set<Long>?,
  override val suggestLanguageIds: Set<Long>?,
) : Serializable,
  IPermission {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PermissionDto

    if (id != other.id) return false
    if (userId != other.userId) return false
    if (invitationId != other.invitationId) return false
    if (!scopes.contentEquals(other.scopes)) return false
    if (projectId != other.projectId) return false
    if (organizationId != other.organizationId) return false
    if (translateLanguageIds != other.translateLanguageIds) return false
    if (suggestLanguageIds != other.suggestLanguageIds) return false
    if (type != other.type) return false
    if (granular != other.granular) return false
    if (viewLanguageIds != other.viewLanguageIds) return false
    if (stateChangeLanguageIds != other.stateChangeLanguageIds) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + (userId?.hashCode() ?: 0)
    result = 31 * result + (invitationId?.hashCode() ?: 0)
    result = 31 * result + scopes.contentHashCode()
    result = 31 * result + (projectId?.hashCode() ?: 0)
    result = 31 * result + (organizationId?.hashCode() ?: 0)
    result = 31 * result + (translateLanguageIds?.hashCode() ?: 0)
    result = 31 * result + (type?.hashCode() ?: 0)
    result = 31 * result + (granular?.hashCode() ?: 0)
    result = 31 * result + (viewLanguageIds?.hashCode() ?: 0)
    result = 31 * result + (stateChangeLanguageIds?.hashCode() ?: 0)
    result = 31 * result + (suggestLanguageIds?.hashCode() ?: 0)
    return result
  }
}
