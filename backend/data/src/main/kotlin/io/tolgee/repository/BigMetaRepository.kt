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
        bmr as bigMeta, 
        kr as key
      from BigMeta bm
      join Key k on bm.keyName = k.name and k.id = :keyId
      left join k.namespace n
      join BigMeta bmr on 
        bm.project = bmr.project and 
        bmr.location = bm.location
        
      left join Key kr on bmr.keyName = kr.name 
      left join fetch kr.namespace nr
      where 
       bm.project.id = :projectId and 
       (kr is null or (nr is null and kr.namespace is null and bmr.namespace is null) or nr.name = bmr.namespace) and
       ((n is null and k.namespace is null and bm.namespace is null) or n.name = bm.namespace)
       group by bmr.id, kr.id, nr.id
       order by bmr.id
    """,
    countQuery = """
      select count(bm) from BigMeta bm
      join Key k on bm.keyName = k.name and k.id = :keyId
      left join k.namespace n

      join BigMeta bmr on 
        bm.project = bmr.project and 
        bmr.location = bm.location
        
      left join Key kr on bmr.keyName = kr.name 
      left join kr.namespace nr
      where 
       bm.project.id = :projectId and 
       k.id = :keyId and
       (kr is null or (nr is null and kr.namespace is null and bmr.namespace is null) or nr.name = bmr.namespace) and
       ((n is null and k.namespace is null and bm.namespace is null) or n.name = bm.namespace)
       group by bmr.id, kr.id, nr.id
       order by bmr.id
    """
  )
  fun getMetasWithSameLocation(projectId: Long, keyId: Long, pageable: Pageable): Page<BigMetaView>

  @Query(
    """
      select 
        bmr
      from BigMeta bm
      join Key k on bm.keyName = k.name and k.id = :keyId
      left join k.namespace n
      join BigMeta bmr on 
        bm.project = bmr.project and 
        bmr.location = bm.location
        
      where 
       bm.project.id = k.project.id and 
       ((n is null and k.namespace is null and bm.namespace is null) or n.name = bm.namespace)
       group by bmr.id
       order by bmr.id
    """
  )
  fun getMetasWithSameLocation(keyId: Long): List<BigMeta>

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
