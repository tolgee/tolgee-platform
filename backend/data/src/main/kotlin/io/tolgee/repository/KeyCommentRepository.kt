package io.tolgee.repository

import io.tolgee.model.key.KeyComment
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Lazy
interface KeyCommentRepository : JpaRepository<KeyComment?, Long?> {
  @Modifying
  @Transactional
  @Query(
    "delete from KeyComment kc " +
      "where kc.keyMeta in (select km from kc.keyMeta km where km.importKey.id in :keyIds)",
  )
  fun deleteAllByImportKeyIds(keyIds: List<Long>)

  @Modifying
  @Transactional
  @Query(
    "delete from KeyComment kc " +
      "where kc.keyMeta in (select km from kc.keyMeta km where km.key.id in :keyIds)",
  )
  fun deleteAllByKeyIds(keyIds: Collection<Long>)

  @Modifying
  @Query(
    "delete from KeyComment kc " +
      "where kc.keyMeta in (select km from kc.keyMeta km where km.key.id in " +
      "(select k.id from Key k where k.project.id = :projectId))",
  )
  fun deleteAllByProject(projectId: Long)
}
