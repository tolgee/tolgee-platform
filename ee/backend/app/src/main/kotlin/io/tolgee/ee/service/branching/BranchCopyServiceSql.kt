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
  override fun copy(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    require(sourceBranch.id != targetBranch.id) { "Source and target branch must differ" }

    createKeyMapping(projectId, sourceBranch)
    copyKeys(projectId, targetBranch)
    copyTranslations(targetBranch)
    copyTranslationLabels()
    copyKeyMetas(targetBranch)
    copyKeyMetaTags()
    copyKeyMetaComments(targetBranch)
    copyKeyMetaCodeReferences(targetBranch)
    copyKeyScreenshotReferences()
    copyTranslationComments(targetBranch)
    copyTranslationQaIssues(targetBranch)
  }

  private fun createKeyMapping(
    projectId: Long,
    sourceBranch: Branch,
  ) {
    entityManager
      .createNativeQuery(
        """
        CREATE TEMPORARY TABLE temp_key_mapping (
          source_key_id BIGINT NOT NULL PRIMARY KEY,
          target_key_id BIGINT NOT NULL
        ) ON COMMIT DROP
        """,
      ).executeUpdate()

    entityManager
      .createNativeQuery(
        """
        INSERT INTO temp_key_mapping (source_key_id, target_key_id)
        SELECT sk.id, nextval('hibernate_sequence')
        FROM (
          SELECT k.id FROM key k
          WHERE k.project_id = :projectId
            AND ${getSourceBranchFilter("k", sourceBranch.isDefault)}
          ORDER BY k.id
        ) sk
        """,
      ).setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .executeUpdate()

    entityManager
      .createNativeQuery("CREATE INDEX idx_temp_key_mapping_target ON temp_key_mapping (target_key_id)")
      .executeUpdate()
    // Temp tables are never auto-analyzed; without stats the planner picks full-table joins.
    entityManager.createNativeQuery("ANALYZE temp_key_mapping").executeUpdate()
  }

  private fun getSourceBranchFilter(
    keyAlias: String,
    sourceIsDefault: Boolean,
  ): String {
    return if (sourceIsDefault) {
      "($keyAlias.branch_id = :sourceBranchId OR $keyAlias.branch_id IS NULL) AND $keyAlias.deleted_at IS NULL"
    } else {
      "$keyAlias.branch_id = :sourceBranchId AND $keyAlias.deleted_at IS NULL"
    }
  }

  /**
   * Copies key metas using the pre-computed key mapping.
   */
  private fun copyKeyMetas(targetBranch: Branch) {
    val sql = """
      INSERT INTO key_meta (id, key_id, description, custom, created_at, updated_at)
      SELECT nextval('hibernate_sequence'), m.target_key_id, km.description, km.custom, 
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM key_meta km
      JOIN temp_key_mapping m ON m.source_key_id = km.key_id
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  /**
   * Copies key meta tags using the pre-computed key mapping.
   */
  private fun copyKeyMetaTags() {
    val sql = """
      INSERT INTO key_meta_tags (key_metas_id, tags_id)
      SELECT tkm.id, kmt.tags_id
      FROM key_meta km
      JOIN temp_key_mapping m ON m.source_key_id = km.key_id
      JOIN key_meta tkm ON tkm.key_id = m.target_key_id
      JOIN key_meta_tags kmt ON kmt.key_metas_id = km.id
    """
    entityManager.createNativeQuery(sql).executeUpdate()
  }

  /**
   * Copies key meta comments using the pre-computed key mapping.
   */
  private fun copyKeyMetaComments(targetBranch: Branch) {
    val sql = """
      INSERT INTO key_comment (id, key_meta_id, author_id, text, from_import, created_at, updated_at)
      SELECT nextval('hibernate_sequence'), tkm.id, kc.author_id, kc.text, kc.from_import, 
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM key_comment kc
      JOIN key_meta km ON km.id = kc.key_meta_id
      JOIN temp_key_mapping m ON m.source_key_id = km.key_id
      JOIN key_meta tkm ON tkm.key_id = m.target_key_id
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  /**
   * Copies key meta code references using the pre-computed key mapping.
   */
  private fun copyKeyMetaCodeReferences(targetBranch: Branch) {
    val sql = """
      INSERT INTO key_code_reference (id, key_meta_id, author_id, path, line, from_import, created_at, updated_at)
      SELECT nextval('hibernate_sequence'), tkm.id, kcr.author_id, kcr.path, kcr.line, kcr.from_import, 
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM key_code_reference kcr
      JOIN key_meta km ON km.id = kcr.key_meta_id
      JOIN temp_key_mapping m ON m.source_key_id = km.key_id
      JOIN key_meta tkm ON tkm.key_id = m.target_key_id
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  /**
   * Copies key screenshot references using the pre-computed key mapping.
   */
  private fun copyKeyScreenshotReferences() {
    val sql = """
      INSERT INTO key_screenshot_reference (key_id, screenshot_id, positions, original_text)
      SELECT m.target_key_id, ksr.screenshot_id, ksr.positions, ksr.original_text
      FROM key_screenshot_reference ksr
      JOIN temp_key_mapping m ON m.source_key_id = ksr.key_id
    """
    entityManager.createNativeQuery(sql).executeUpdate()
  }

  /**
   * Copies translation comments using the pre-computed key mapping.
   */
  private fun copyTranslationComments(targetBranch: Branch) {
    val sql = """
      INSERT INTO translation_comment (id, text, state, translation_id, author_id, created_at, updated_at)
      SELECT nextval('hibernate_sequence'), tc.text, tc.state, tgt_t.id, tc.author_id, 
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM translation_comment tc
      JOIN translation src_t ON src_t.id = tc.translation_id
      JOIN temp_key_mapping m ON m.source_key_id = src_t.key_id
      JOIN translation tgt_t ON tgt_t.key_id = m.target_key_id 
                            AND tgt_t.language_id = src_t.language_id
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  private fun copyKeys(
    projectId: Long,
    targetBranch: Branch,
  ) {
    val sql = """
      INSERT INTO key (id, name, project_id, namespace_id, branch_id, is_plural, plural_arg_name, max_char_limit, created_at, updated_at)
      SELECT m.target_key_id, k.name, :projectId, k.namespace_id, :targetBranchId,
             k.is_plural, k.plural_arg_name, k.max_char_limit,
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM temp_key_mapping m
      JOIN key k ON k.id = m.source_key_id
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("targetBranchId", targetBranch.id)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  /**
   * Copies translations using the pre-computed key mapping.
   */
  private fun copyTranslations(targetBranch: Branch) {
    val sql = """
      INSERT INTO translation (
        id, text, key_id, language_id, state, auto, mt_provider,
        word_count, character_count, outdated, qa_checks_stale, created_at, updated_at
      )
      SELECT nextval('hibernate_sequence') AS id,
             t.text, m.target_key_id, t.language_id, t.state, t.auto, t.mt_provider,
             t.word_count, t.character_count, t.outdated, t.qa_checks_stale,
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM translation t
      JOIN temp_key_mapping m ON m.source_key_id = t.key_id
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  /**
   * Copies QA issues using the pre-computed key mapping.
   */
  private fun copyTranslationQaIssues(targetBranch: Branch) {
    val sql = """
      INSERT INTO translation_qa_issue (
        id, type, message, replacement, position_start, position_end, state, params,
        virtual, plural_variant, translation_id, created_at, updated_at
      )
      SELECT nextval('hibernate_sequence') AS id,
             qi.type, qi.message, qi.replacement, qi.position_start, qi.position_end,
             qi.state, qi.params, qi.virtual, qi.plural_variant, tgt_t.id,
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM translation_qa_issue qi
      JOIN translation src_t ON src_t.id = qi.translation_id
      JOIN temp_key_mapping m ON m.source_key_id = src_t.key_id
      JOIN translation tgt_t ON tgt_t.key_id = m.target_key_id
                            AND tgt_t.language_id = src_t.language_id
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  /**
   * Copies translation labels using the pre-computed key mapping.
   */
  private fun copyTranslationLabels() {
    val sql = """
      INSERT INTO translation_label (translation_id, label_id)
      SELECT tgt_t.id, tl.label_id
      FROM translation src_t
      JOIN temp_key_mapping m ON m.source_key_id = src_t.key_id
      JOIN translation tgt_t ON tgt_t.key_id = m.target_key_id 
                            AND tgt_t.language_id = src_t.language_id
      JOIN translation_label tl ON tl.translation_id = src_t.id
    """
    entityManager.createNativeQuery(sql).executeUpdate()
  }
}
