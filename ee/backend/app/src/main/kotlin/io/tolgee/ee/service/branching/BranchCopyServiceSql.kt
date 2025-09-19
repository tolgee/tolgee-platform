package io.tolgee.ee.service.branching

import io.tolgee.model.branching.Branch
import io.tolgee.service.branching.BranchCopyService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class BranchCopyServiceSql(
  private val entityManager: EntityManager,
) : BranchCopyService {

  /**
   * Copies keys and its related entities from a source branch to target branch
   * - translations
   * - translation labels
   * If sourceBranch is a default, keys with NULL branch_id are also treated as part of the source
   */
  @Transactional
  override fun copy(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    require(sourceBranch.id != targetBranch.id) { "Source and target branch must differ" }

    copyKeys(projectId, sourceBranch, targetBranch)
    copyTranslations(projectId, sourceBranch, targetBranch)
    copyTranslationLabels(projectId, sourceBranch, targetBranch)
  }

  private fun copyKeys(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    val sql = """
      with source_keys as (
        select k.id as old_id, k.name, k.namespace_id, k.is_plural, k.plural_arg_name
        from key k
        where k.project_id = :projectId
          and (
            (:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
            or (not :sourceIsDefault and k.branch_id = :sourceBranchId)
          )
      ), existing_target as (
        select tk.id, tk.name, tk.namespace_id
        from key tk
        where tk.project_id = :projectId and tk.branch_id = :targetBranchId
      ), to_insert as (
        select sk.* from source_keys sk
        left join existing_target et on et.name = sk.name and coalesce(et.namespace_id,0) = coalesce(sk.namespace_id,0)
        where et.id is null
      )
      insert into key (id, name, project_id, namespace_id, branch_id, is_plural, plural_arg_name, created_at, updated_at)
      select nextval('hibernate_sequence') as id,
             ti.name, :projectId, ti.namespace_id, :targetBranchId, ti.is_plural, ti.plural_arg_name, now(), now()
      from to_insert ti
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  private fun copyTranslations(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    val sql = """
      with source_keys as (
        select k.id as old_id, k.name, k.namespace_id
        from key k
        where k.project_id = :projectId
          and (
            (:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
            or (not :sourceIsDefault and k.branch_id = :sourceBranchId)
          )
      ), target_keys as (
        select tk.id, tk.name, tk.namespace_id
        from key tk
        where tk.project_id = :projectId and tk.branch_id = :targetBranchId
      ), key_id_map as (
        select sk.old_id as old_key_id, tk.id as new_key_id
        from source_keys sk
        join target_keys tk on tk.name = sk.name and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      )
      insert into translation (
        id, text, key_id, language_id, state, auto, mt_provider, word_count, character_count, outdated, created_at, updated_at
      )
      select nextval('hibernate_sequence') as id,
             t.text, m.new_key_id, t.language_id, t.state, t.auto, t.mt_provider,
             t.word_count, t.character_count, t.outdated, now(), now()
      from translation t
      join key_id_map m on m.old_key_id = t.key_id
      left join translation existing on existing.key_id = m.new_key_id and existing.language_id = t.language_id
      where existing.id is null
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  private fun copyTranslationLabels(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    val sql = """
      insert into translation_label (translation_id, label_id)
      select tgt_t.id, tl.label_id
      from translation src_t
      join key sk on sk.id = src_t.key_id and sk.project_id = :projectId
      join key tk on tk.project_id = sk.project_id 
          and tk.branch_id = :targetBranchId 
          and tk.name = sk.name
          and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      join translation tgt_t on tgt_t.key_id = tk.id and tgt_t.language_id = src_t.language_id
      join translation_label tl on tl.translation_id = src_t.id
      where (
        (:sourceIsDefault and (sk.branch_id = :sourceBranchId or sk.branch_id is null))
        or (not :sourceIsDefault and sk.branch_id = :sourceBranchId)
      )
      and not exists (
        select 1 from translation_label existing where existing.translation_id = tgt_t.id and existing.label_id = tl.label_id
      )
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }
}
