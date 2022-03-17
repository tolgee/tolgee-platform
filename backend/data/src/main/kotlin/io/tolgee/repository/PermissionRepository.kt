package io.tolgee.repository

import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PermissionRepository : JpaRepository<Permission, Long> {
  fun findOneByProjectIdAndUserId(projectId: Long?, userId: Long?): Permission?

  fun getAllByProjectAndUserNotNull(project: io.tolgee.model.Project?): Set<Permission>

  @Query("from Permission p join Project r on r = p.project where p.user = ?1 order by r.name")
  fun findAllByUser(userAccount: UserAccount?): LinkedHashSet<Permission>

  fun deleteByIdIn(ids: Collection<Long>)

  @Query("select p.id from Permission p where p.project.id = :projectId")
  fun getIdsByProject(projectId: Long): List<Long>

  @Query(
    """select distinct p
    from Permission p
    join p.languages l on l = :language
    join fetch p.languages allLangs
  """
  )
  fun findAllByPermittedLanguage(language: Language): List<Permission>

  @Query(
    """
      select p.user.id, l.id from Permission p
      join p.languages l
      where p.user.id in :userIds
    """
  )
  fun getUserPermittedLanguageIds(userIds: List<Long>): List<Array<Long>>

  @Query(
    """
      select p.project.id, l.id from Permission p
      join p.languages l
      where p.project.id in :projectIds
      and p.user.id = :userId
    """
  )
  fun getProjectPermittedLanguageIds(projectIds: List<Long>, userId: Long): List<Array<Long>>
}
