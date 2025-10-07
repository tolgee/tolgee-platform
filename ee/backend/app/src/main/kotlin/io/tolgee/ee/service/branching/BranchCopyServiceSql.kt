package io.tolgee.ee.service.branching

import io.tolgee.model.branching.Branch
import io.tolgee.service.branching.BranchCopyService
import io.tolgee.util.Logging
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class BranchCopyServiceSql(
  private val entityManager: EntityManager,
) : BranchCopyService, Logging {

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

    traceLogMeasureTime("branchCopyService: copyKeys") {
      copyKeys(projectId, sourceBranch, targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyTranslations") {
      copyTranslations(projectId, sourceBranch, targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyTranslationLabels") {
      copyTranslationLabels(projectId, sourceBranch, targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyKeyMetas") {
      copyKeyMetas(projectId, sourceBranch, targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyKeyMetaTags") {
      copyKeyMetaTags(projectId, sourceBranch, targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyKeyMetaComments") {
      copyKeyMetaComments(projectId, sourceBranch, targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyKeyMetaCodeReferences") {
      copyKeyMetaCodeReferences(projectId, sourceBranch, targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyKeyScreenshotReferences") {
      copyKeyScreenshotReferences(projectId, sourceBranch, targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyTranslationComments") {
      copyTranslationComments(projectId, sourceBranch, targetBranch)
    }
  }

  private fun copyKeyMetas(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    val sql = """
      insert into key_meta (id, key_id, description, custom, created_at, updated_at)
      select nextval('hibernate_sequence'), tk.id, km.description, km.custom, km.created_at, km.updated_at
      from key_meta km
      join key sk on sk.id = km.key_id and sk.project_id = :projectId
      join key tk on tk.project_id = sk.project_id
                 and tk.branch_id = :targetBranchId
                 and tk.name = sk.name
                 and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      where ((:sourceIsDefault and (sk.branch_id = :sourceBranchId or sk.branch_id is null))
             or (not :sourceIsDefault and sk.branch_id = :sourceBranchId))
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
      insert into key_meta_tags (key_metas_id, tags_id)
      select tkm.id, kmt.tags_id
      from key_meta km
      join key sk on sk.id = km.key_id and sk.project_id = :projectId
      join key tk on tk.project_id = sk.project_id
                 and tk.name = sk.name
                 and tk.branch_id = :targetBranchId
                 and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      join key_meta tkm on tkm.key_id = tk.id
      join key_meta_tags kmt on kmt.key_metas_id = km.id
      where ((:sourceIsDefault and (sk.branch_id = :sourceBranchId or sk.branch_id is null))
             or (not :sourceIsDefault and sk.branch_id = :sourceBranchId))
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
      insert into key_comment (id, key_meta_id, author_id, text, from_import, created_at, updated_at)
      select nextval('hibernate_sequence'), tkm.id, kc.author_id, kc.text, kc.from_import, kc.created_at, kc.updated_at
      from key_comment kc
      join key_meta km on km.id = kc.key_meta_id
      join key sk on sk.id = km.key_id and sk.project_id = :projectId
      join key tk on tk.project_id = sk.project_id
                 and tk.branch_id = :targetBranchId
                 and tk.name = sk.name
                 and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      join key_meta tkm on tkm.key_id = tk.id
      where ((:sourceIsDefault and (sk.branch_id = :sourceBranchId or sk.branch_id is null))
             or (not :sourceIsDefault and sk.branch_id = :sourceBranchId))
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
      insert into key_code_reference (id, key_meta_id, author_id, path, line, from_import, created_at, updated_at)
      select nextval('hibernate_sequence'), tkm.id, kcr.author_id, kcr.path, kcr.line, kcr.from_import, kcr.created_at, kcr.updated_at
      from key_code_reference kcr
      join key_meta km on km.id = kcr.key_meta_id
      join key sk on sk.id = km.key_id and sk.project_id = :projectId
      join key tk on tk.project_id = sk.project_id
                 and tk.branch_id = :targetBranchId
                 and tk.name = sk.name
                 and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      join key_meta tkm on tkm.key_id = tk.id
      where ((:sourceIsDefault and (sk.branch_id = :sourceBranchId or sk.branch_id is null))
             or (not :sourceIsDefault and sk.branch_id = :sourceBranchId))
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
      insert into key_screenshot_reference (key_id, screenshot_id, positions, original_text)
      select tk.id, ksr.screenshot_id, ksr.positions, ksr.original_text
      from key_screenshot_reference ksr
      join key sk on sk.id = ksr.key_id and sk.project_id = :projectId
      join key tk on tk.project_id = sk.project_id
                 and tk.branch_id = :targetBranchId
                 and tk.name = sk.name
                 and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      where ((:sourceIsDefault and (sk.branch_id = :sourceBranchId or sk.branch_id is null))
             or (not :sourceIsDefault and sk.branch_id = :sourceBranchId))
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
      insert into translation_comment (id, text, state, translation_id, author_id, created_at, updated_at)
      select nextval('hibernate_sequence'), tc.text, tc.state, tgt_t.id, tc.author_id, tc.created_at, tc.updated_at
      from translation_comment tc
      join translation src_t on src_t.id = tc.translation_id
      join key sk on sk.id = src_t.key_id and sk.project_id = :projectId
      join key tk on tk.project_id = sk.project_id 
                 and tk.branch_id = :targetBranchId 
                 and tk.name = sk.name
                 and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      join translation tgt_t on tgt_t.key_id = tk.id and tgt_t.language_id = src_t.language_id
      where (
        (:sourceIsDefault and (sk.branch_id = :sourceBranchId or sk.branch_id is null))
        or (not :sourceIsDefault and sk.branch_id = :sourceBranchId)
      )
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
        select k.id as old_id, k.name, k.namespace_id, k.is_plural, k.plural_arg_name, k.created_at, k.updated_at
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
             ti.name, :projectId, ti.namespace_id, :targetBranchId, ti.is_plural, ti.plural_arg_name, ti.created_at, ti.updated_at
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
      insert into translation (
        id, text, key_id, language_id, state, auto, mt_provider, word_count, character_count, outdated, created_at, updated_at
      )
      select nextval('hibernate_sequence') as id,
             t.text, tk.id, t.language_id, t.state, t.auto, t.mt_provider,
             t.word_count, t.character_count, t.outdated, t.created_at, t.updated_at
      from translation t
      join key sk on sk.id = t.key_id and sk.project_id = :projectId
      join key tk on tk.project_id = sk.project_id 
                 and tk.branch_id = :targetBranchId 
                 and tk.name = sk.name
                 and coalesce(tk.namespace_id,0) = coalesce(sk.namespace_id,0)
      where (
        (:sourceIsDefault and (sk.branch_id = :sourceBranchId or sk.branch_id is null))
        or (not :sourceIsDefault and sk.branch_id = :sourceBranchId)
      )
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
    """
    entityManager.createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("sourceIsDefault", sourceBranch.isDefault)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }
}
