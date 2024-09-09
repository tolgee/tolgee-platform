package io.tolgee.repository

import io.tolgee.model.key.KeyMeta
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Lazy
interface KeyMetaRepository : JpaRepository<KeyMeta?, Long?> {
  @Modifying
  @Transactional
  @Query(
    """delete from KeyComment kc where kc.keyMeta in 
        (select km from kc.keyMeta km join km.key k where k.project.id = :projectId)""",
  )
  fun deleteAllKeyCommentsByProjectId(projectId: Long)

  @Modifying
  @Transactional
  @Query(
    """delete from KeyCodeReference kcr where kcr.keyMeta in 
        (select km from kcr.keyMeta km join km.key k where k.project.id = :projectId)""",
  )
  fun deleteAllKeyCodeReferencesByProjectId(projectId: Long)

  @Modifying
  @Transactional
  @Query("delete from KeyMeta km where km.key in (select k from km.key k where k.project.id = :projectId)")
  fun deleteAllByProjectId(projectId: Long)

  @Modifying
  @Transactional
  @Query("delete from KeyMeta km where km.importKey.id in :keyIds")
  fun deleteAllByImportKeyIdIn(keyIds: List<Long>)

  @Modifying
  @Transactional
  @Query("delete from KeyMeta km where km.key.id in :keyIds")
  fun deleteAllByKeyIds(keyIds: Collection<Long>)

  @Modifying
  @Query("delete from KeyMeta km where km.key.id in (select k.id from Key k where k.project.id = :projectId)")
  fun deleteAllByProject(projectId: Long)
}
