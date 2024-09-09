package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportKey
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Lazy
interface ImportKeyRepository : JpaRepository<ImportKey, Long> {
  @Query(
    """
        select distinct ik from ImportKey ik 
          left join fetch ik.keyMeta km
          join fetch ik.file if 
          join fetch if.import im where im.id = :importId
        """,
  )
  fun findAllByImport(importId: Long): List<ImportKey>

  @Modifying
  @Transactional
  @Query("""delete from ImportKey ik where ik.id in :ids""")
  fun deleteByIdIn(ids: List<Long>)

  @Query("""select iik.id from ImportKey iik join iik.file if where if.import = :import""")
  fun getAllIdsByImport(import: Import): List<Long>
}
