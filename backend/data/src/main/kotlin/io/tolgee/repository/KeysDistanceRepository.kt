package io.tolgee.repository

import io.tolgee.model.keyBigMeta.KeysDistance
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface KeysDistanceRepository : JpaRepository<KeysDistance, Long> {
  @Query("""from KeysDistance kd where kd.key1Id in :data or kd.key2Id in :data""")
  fun findForKeyIds(data: Collection<Long>): List<KeysDistance>

  @Query(
    """
    select kd.key1Id, kd.key2Id from KeysDistance kd 
    where kd.key1Id = :keyId or 
          kd.key2Id = :keyId 
    order by kd.score desc
        """
  )
  fun getCloseKeys(keyId: Long, pageable: Pageable = PageRequest.of(0, 10)): List<Array<Long>>
  fun deleteAllByProjectId(id: Long)
}
