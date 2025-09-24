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
   * - key metas (description, custom) + tags
   * - key meta comments & code references
   * - key screenshot references
   * - translation comments
   * If sourceBranch is a default, keys with NULL branch_id are also treated as part of the source
   */
  @Transactional
  override fun copy(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    require(sourceBranch.id != targetBranch.id) { "Source and target branch must differ" }

    copyKeys(projectId, sourceBranch, targetBranch)
    copyTranslations(projectId, sourceBranch, targetBranch)
    copyTranslationLabels(projectId, sourceBranch, targetBranch)
    copyKeyMetas(projectId, sourceBranch, targetBranch)
    copyKeyMetaTags(projectId, sourceBranch, targetBranch)
    copyKeyMetaComments(projectId, sourceBranch, targetBranch)
    copyKeyMetaCodeReferences(projectId, sourceBranch, targetBranch)
    copyKeyScreenshotReferences(projectId, sourceBranch, targetBranch)
    copyTranslationComments(projectId, sourceBranch, targetBranch)
  }

  private fun copyKeyMetas(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    val sql = """
      with source_keys as (
        select k.id as old_id, k.name, k.namespace_id
        from key k
        where k.project_id = :projectId
          and ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      ), target_keys as (
        select tk.id, tk.name, tk.namespace_id
        from key tk
        where tk.project_id = :projectId and tk.branch_id = :targetBranchId
      ), key_map as (
        select sk.old_id as old_key_id, tk.id as new_key_id
        from source_keys sk
        join target_keys tk on tk.name = sk.name and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      ), source_metas as (
        select km.id as old_meta_id, km.key_id, km.description, km.custom
        from key_meta km
        join key k on k.id = km.key_id and k.project_id = :projectId
        where ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      )
      insert into key_meta (id, key_id, description, custom, created_at, updated_at)
      select nextval('hibernate_sequence'), m.new_key_id, sm.description, sm.custom, now(), now()
      from source_metas sm
      join key_map m on m.old_key_id = sm.key_id
      left join key_meta existing on existing.key_id = m.new_key_id
      where existing.id is null
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  private fun copyKeyMetaTags(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    val sql = """
      with source_keys as (
        select k.id as old_id, k.name, k.namespace_id
        from key k
        where k.project_id = :projectId
          and ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      ), target_keys as (
        select tk.id, tk.name, tk.namespace_id
        from key tk
        where tk.project_id = :projectId and tk.branch_id = :targetBranchId
      ), key_map as (
        select sk.old_id as old_key_id, tk.id as new_key_id
        from source_keys sk
        join target_keys tk on tk.name = sk.name and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      ), source_meta as (
        select km.id as old_meta_id, km.key_id
        from key_meta km
        join key k on k.id = km.key_id and k.project_id = :projectId
        where ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      ), target_meta as (
        select km.id as new_meta_id, km.key_id
        from key_meta km
        join key k on k.id = km.key_id and k.project_id = :projectId and k.branch_id = :targetBranchId
      ), meta_map as (
        select sm.old_meta_id, tm.new_meta_id
        from source_meta sm
        join key_map km on km.old_key_id = sm.key_id
        join target_meta tm on tm.key_id = km.new_key_id
      )
      insert into key_meta_tags (key_metas_id, tags_id)
      select mm.new_meta_id, kmt.tags_id
      from key_meta_tags kmt
      join meta_map mm on mm.old_meta_id = kmt.key_metas_id
      left join key_meta_tags existing on existing.key_metas_id = mm.new_meta_id and existing.tags_id = kmt.tags_id
      where existing.key_metas_id is null
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  private fun copyKeyMetaComments(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    val sql = """
      with source_keys as (
        select k.id as old_id, k.name, k.namespace_id
        from key k
        where k.project_id = :projectId
          and ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      ), target_keys as (
        select tk.id, tk.name, tk.namespace_id
        from key tk
        where tk.project_id = :projectId and tk.branch_id = :targetBranchId
      ), key_map as (
        select sk.old_id as old_key_id, tk.id as new_key_id
        from source_keys sk
        join target_keys tk on tk.name = sk.name and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      ), source_meta as (
        select km.id as old_meta_id, km.key_id
        from key_meta km
        join key k on k.id = km.key_id and k.project_id = :projectId
        where ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      ), target_meta as (
        select km.id as new_meta_id, km.key_id
        from key_meta km
        join key k on k.id = km.key_id and k.project_id = :projectId and k.branch_id = :targetBranchId
      ), meta_map as (
        select sm.old_meta_id, tm.new_meta_id
        from source_meta sm
        join key_map km on km.old_key_id = sm.key_id
        join target_meta tm on tm.key_id = km.new_key_id
      )
      insert into key_comment (id, key_meta_id, author_id, text, from_import, created_at, updated_at)
      select nextval('hibernate_sequence'), mm.new_meta_id, kc.author_id, kc.text, kc.from_import, now(), now()
      from key_comment kc
      join meta_map mm on mm.old_meta_id = kc.key_meta_id
      left join key_comment existing on existing.key_meta_id = mm.new_meta_id and existing.author_id = kc.author_id and existing.text = kc.text
      where existing.id is null
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  private fun copyKeyMetaCodeReferences(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    val sql = """
      with source_keys as (
        select k.id as old_id, k.name, k.namespace_id
        from key k
        where k.project_id = :projectId
          and ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      ), target_keys as (
        select tk.id, tk.name, tk.namespace_id
        from key tk
        where tk.project_id = :projectId and tk.branch_id = :targetBranchId
      ), key_map as (
        select sk.old_id as old_key_id, tk.id as new_key_id
        from source_keys sk
        join target_keys tk on tk.name = sk.name and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      ), source_meta as (
        select km.id as old_meta_id, km.key_id
        from key_meta km
        join key k on k.id = km.key_id and k.project_id = :projectId
        where ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      ), target_meta as (
        select km.id as new_meta_id, km.key_id
        from key_meta km
        join key k on k.id = km.key_id and k.project_id = :projectId and k.branch_id = :targetBranchId
      ), meta_map as (
        select sm.old_meta_id, tm.new_meta_id
        from source_meta sm
        join key_map km on km.old_key_id = sm.key_id
        join target_meta tm on tm.key_id = km.new_key_id
      )
      insert into key_code_reference (id, key_meta_id, author_id, path, line, from_import, created_at, updated_at)
      select nextval('hibernate_sequence'), mm.new_meta_id, kcr.author_id, kcr.path, kcr.line, kcr.from_import, now(), now()
      from key_code_reference kcr
      join meta_map mm on mm.old_meta_id = kcr.key_meta_id
      left join key_code_reference existing on existing.key_meta_id = mm.new_meta_id and existing.author_id = kcr.author_id and existing.path = kcr.path and coalesce(existing.line, -1) = coalesce(kcr.line, -1)
      where existing.id is null
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  private fun copyKeyScreenshotReferences(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    val sql = """
      with source_keys as (
        select k.id as old_id, k.name, k.namespace_id
        from key k
        where k.project_id = :projectId
          and ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      ), target_keys as (
        select tk.id, tk.name, tk.namespace_id
        from key tk
        where tk.project_id = :projectId and tk.branch_id = :targetBranchId
      ), key_map as (
        select sk.old_id as old_key_id, tk.id as new_key_id
        from source_keys sk
        join target_keys tk on tk.name = sk.name and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      )
      insert into key_screenshot_reference (key_id, screenshot_id, positions, original_text)
      select km.new_key_id, ksr.screenshot_id, ksr.positions, ksr.original_text
      from key_screenshot_reference ksr
      join key_map km on km.old_key_id = ksr.key_id
      left join key_screenshot_reference existing on existing.key_id = km.new_key_id and existing.screenshot_id = ksr.screenshot_id
      where existing.key_id is null
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  private fun copyTranslationComments(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    val sql = """
      with source_keys as (
        select k.id as old_id, k.name, k.namespace_id
        from key k
        where k.project_id = :projectId
          and ((:sourceIsDefault and (k.branch_id = :sourceBranchId or k.branch_id is null))
               or (not :sourceIsDefault and k.branch_id = :sourceBranchId))
      ), target_keys as (
        select tk.id, tk.name, tk.namespace_id
        from key tk
        where tk.project_id = :projectId and tk.branch_id = :targetBranchId
      ), key_map as (
        select sk.old_id as old_key_id, tk.id as new_key_id
        from source_keys sk
        join target_keys tk on tk.name = sk.name and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      ), translation_map as (
        select st.id as old_tr_id, tt.id as new_tr_id
        from translation st
        join key_map km on km.old_key_id = st.key_id
        join translation tt on tt.key_id = km.new_key_id and tt.language_id = st.language_id
      )
      insert into translation_comment (id, text, state, translation_id, author_id, created_at, updated_at)
      select nextval('hibernate_sequence'), tc.text, tc.state, tm.new_tr_id, tc.author_id, now(), now()
      from translation_comment tc
      join translation_map tm on tm.old_tr_id = tc.translation_id
      left join translation_comment existing on existing.translation_id = tm.new_tr_id and existing.author_id = tc.author_id and existing.text = tc.text and existing.state = tc.state
      where existing.id is null
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
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
