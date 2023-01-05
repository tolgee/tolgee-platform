package io.tolgee.repository

import io.tolgee.model.Language
import io.tolgee.model.Permission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PermissionRepository : JpaRepository<Permission, Long> {

  @Query(
    """
    from Permission p 
    where 
        ((:projectId is null and p.project.id is null) or p.project.id = :projectId) and 
        ((:userId is null and p.user.id is null) or p.user.id = :userId) and 
        ((:organizationId is null and p.organization.id is null) or p.organization.id = :organizationId)
  """
  )
  fun findOneByProjectIdAndUserIdAndOrganizationId(
    projectId: Long?,
    userId: Long?,
    organizationId: Long? = null
  ): Permission?

  fun getAllByProjectAndUserNotNull(project: io.tolgee.model.Project?): Set<Permission>

  fun deleteByIdIn(ids: Collection<Long>)

  @Query("select p.id from Permission p where p.project.id = :projectId")
  fun getIdsByProject(projectId: Long): List<Long>

  @Query(
    """select distinct p
    from Permission p
    join p.translateLanguages l on l = :language
    join fetch p.translateLanguages allLangs
  """
  )
  fun findAllByPermittedLanguage(language: Language): List<Permission>

  @Query(
    """
      select p.user.id, l.id from Permission p
      join p.translateLanguages l
      where p.user.id in :userIds
      and p.project.id = :projectId
    """
  )
  fun getUserPermittedLanguageIds(userIds: List<Long>, projectId: Long): List<Array<Long>>

  @Query(
    """
      select p.project.id, l.id from Permission p
      join p.translateLanguages l
      where p.project.id in :projectIds
      and p.user.id = :userId
    """
  )
  fun getProjectPermittedLanguageIds(projectIds: List<Long>, userId: Long): List<Array<Long>>

  @Query(
    """
      from Permission p where p.organization.id in :ids
    """
  )
  fun getOrganizationBasePermissions(ids: Iterable<Long>): List<Permission>
}
