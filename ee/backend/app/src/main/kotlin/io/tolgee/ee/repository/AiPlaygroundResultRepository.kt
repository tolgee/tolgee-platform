package io.tolgee.ee.repository

import io.tolgee.model.AiPlaygroundResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AiPlaygroundResultRepository : JpaRepository<AiPlaygroundResult, Long> {
  @Query(
    """
    from AiPlaygroundResult r
    right join fetch Key k on r.key.id = k.id
    where 
      k.id in :keyIds
      and k.project.id = :projectId
      and r.user.id = :userId
      and r.language.id in :languageIds
    """,
  )
  fun getResults(
    projectId: Long,
    userId: Long,
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): Collection<AiPlaygroundResult>

  @Query(
    """
    from AiPlaygroundResult r
    right join fetch Key k on r.key.id = k.id
    where 
      k.id = :keyId
      and k.project.id = :projectId
      and r.user.id = :userId
      and r.language.id = :languageId
    """,
  )
  fun getResult(
    projectId: Long,
    userId: Long,
    keyId: Long,
    languageId: Long,
  ): AiPlaygroundResult?

  @Modifying
  @Query(
    """
    delete from AiPlaygroundResult r
    where
      r.project.id = :projectId
      and r.user.id = :userId
    """,
  )
  fun removeResults(
    projectId: Long,
    userId: Long,
  )

  @Modifying
  @Query(
    """
    delete from AiPlaygroundResult r
    where r.language.id = :languageId
    """,
  )
  fun deleteAiPlaygroundResultsByLanguage(languageId: Long)

  @Modifying
  @Query(
    """
    delete from AiPlaygroundResult r
    where r.project.id = :projectId
    """,
  )
  fun deleteAiPlaygroundResultsByProject(projectId: Long)

  @Modifying
  @Query(
    """
    delete from AiPlaygroundResult r
    where r.user.id = :userId
    """,
  )
  fun deleteAiPlaygroundResultsByUser(userId: Long)

  @Modifying
  @Query(
    """
    delete from AiPlaygroundResult r
    where r.key.id in :keys
    """,
  )
  fun deleteAiPlaygroundResultsByKeys(keys: Collection<Long>)
}
