package io.tolgee.repository

import io.tolgee.model.keyBigMeta.KeysDistance
import io.tolgee.service.key.KeyWithBaseTranslationView
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface KeysDistanceRepository : JpaRepository<KeysDistance, Long> {
  @Query(
    """
    select (case when kd.key1Id = :keyId then kd.key2Id else kd.key1Id end) from KeysDistance kd 
    where kd.key1Id = :keyId or 
          kd.key2Id = :keyId 
    order by kd.score desc
        """,
  )
  fun getCloseKeys(
    keyId: Long,
    pageable: Pageable = PageRequest.of(0, 10),
  ): List<Long>

  @Query(
    """
    select 
      k.id as id, k.name as name, n.name as namespace, t.text as baseTranslation
    from Key k
    left join k.namespace n
    left join k.translations t on t.language in (select p.baseLanguage from Project p where p.id = :projectId)
    where k.id in (
      select (case when kd.key1Id = :keyId then kd.key2Id else kd.key1Id end) from KeysDistance kd 
      where kd.key1Id = :keyId or 
            kd.key2Id = :keyId
      order by kd.score desc
    ) and k.project.id = :projectId
    """,
  )
  fun getCloseKeysWithBaseTranslation(
    keyId: Long,
    projectId: Long,
    pageable: Pageable = PageRequest.of(0, 10),
  ): List<KeyWithBaseTranslationView>

  fun deleteAllByProjectId(id: Long)
}
