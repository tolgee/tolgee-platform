package io.tolgee.repository

import io.tolgee.model.Project
import io.tolgee.model.keyBigMeta.BigMeta
import io.tolgee.model.views.BigMetaView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BigMetaRepository : JpaRepository<BigMeta, Long> {

  @Query(
    """
      select 
        bm as bigMeta,
        k as key
      from BigMeta bm
      join Key k on bm.keyName = k.name and k.id = :keyId
      left join fetch k.namespace n
        
      where 
       bm.project.id = k.project.id and k.project.id = :projectId and
       ((n is null and k.namespace is null and bm.namespace is null) or n.name = bm.namespace)
       group by bm.id, k.id, n.id
       order by bm.id
    """,
    countQuery = """
      select count(bm) from BigMeta bm
      join Key k on bm.keyName = k.name and k.id = :keyId
      left join k.namespace n
        
      where 
       bm.project.id = k.project.id and k.project.id = :projectId and
       ((n is null and k.namespace is null and bm.namespace is null) or n.name = bm.namespace)
       group by bm.id, k.id, n.id
    """
  )
  fun getMetas(projectId: Long, keyId: Long, pageable: Pageable): Page<BigMetaView>

  @Query(
    """
      select 
        bm
      from BigMeta bm
      join Key k on bm.keyName = k.name and k.id = :keyId
      left join k.namespace n
        
      where 
       bm.project.id = k.project.id and 
       ((n is null and k.namespace is null and bm.namespace is null) or n.name = bm.namespace)
       group by bm.id
       order by bm.id
    """
  )
  fun getMetas(keyId: Long): List<BigMeta>

  @Query(
    """
       select 
        bm as bigMeta, 
        k as key
      from BigMeta bm
      left join fetch Key k on bm.keyName = k.name 
      left join fetch k.namespace n
      where 
       bm.project.id = :projectId and
       (k is null or (n is null and k.namespace is null and bm.namespace is null) or n.name = bm.namespace)
       order by bm.id
    """,
    countQuery = """
       select
        count(bm)
      from BigMeta bm
      left join Key k on bm.keyName = k.name 
      left join k.namespace n
      where 
       bm.project.id = :projectId and
       (k is null or (n is null and k.namespace is null and bm.namespace is null) or n.name = bm.namespace)
    """
  )
  fun findAllByProjectId(projectId: Long, pageable: Pageable): Page<BigMetaView>

  fun findOneByKeyNameAndNamespaceAndLocationAndProject(
    keyName: String,
    namespace: String?,
    location: String,
    project: Project
  ): BigMeta?
}
