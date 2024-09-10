package io.tolgee.repository

import io.tolgee.model.Language
import io.tolgee.model.Permission
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface PermissionRepository : JpaRepository<Permission, Long> {
  @Query(
    """
    from Permission p 
    where 
        ((:projectId is null and p.project.id is null) or p.project.id = :projectId) and 
        ((:userId is null and p.user.id is null) or p.user.id = :userId) and 
        ((:organizationId is null and p.organization.id is null) or p.organization.id = :organizationId)
  """,
  )
  fun findOneByProjectIdAndUserIdAndOrganizationId(
    projectId: Long?,
    userId: Long?,
    organizationId: Long? = null,
  ): Permission?

  fun getAllByProjectAndUserNotNull(project: io.tolgee.model.Project?): Set<Permission>

  fun deleteByIdIn(ids: Collection<Long>)

  @Query("select p.id from Permission p where p.project.id = :projectId")
  fun getIdsByProject(projectId: Long): List<Long>

  @Query(
    """select p from Permission p
        left join fetch p.viewLanguages
        left join fetch p.translateLanguages
        left join fetch p.stateChangeLanguages
        where p.project.id = :projectId""",
  )
  fun getByProjectWithFetchedLanguages(projectId: Long): List<Permission>

  @Query(
    """select distinct p
    from Permission p
    left join p.translateLanguages tl on tl = :language
    left join p.viewLanguages vl on vl = :language
    left join p.stateChangeLanguages scl on scl = :language
    where tl.id is not null or vl.id is not null or scl.id is not null
  """,
  )
  fun findAllByPermittedLanguage(language: Language): List<Permission>

  @Query(
    """
      select p.user.id, l.id from Permission p
      join p.translateLanguages l
      where p.user.id in :userIds
      and p.project.id = :projectId
    """,
  )
  fun getUserPermittedLanguageIds(
    userIds: List<Long>,
    projectId: Long,
  ): List<Array<Long>>

  @Query(
    """
      select p.project.id, l.id from Permission p
      join p.translateLanguages l
      where p.project.id in :projectIds
      and p.user.id = :userId
    """,
  )
  fun getProjectPermittedLanguageIds(
    projectIds: List<Long>,
    userId: Long,
  ): List<Array<Long>>

  @Query(
    """
      from Permission p where p.organization.id in :ids
    """,
  )
  fun getOrganizationBasePermissions(ids: Iterable<Long>): List<Permission>

  @Query(
    """
    from Permission p 
    join p.project pr
    join pr.organizationOwner oo on oo.id = :organizationId 
    where p.user.id = :userId
  """,
  )
  fun findAllByOrganizationAndUserId(
    organizationId: Long,
    userId: Long,
  ): List<Permission>
}
