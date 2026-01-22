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
) : BranchCopyService,
  Logging {
  /**
   * Copies keys and its related entities from a source branch to target branch
   * - translations
   * - translation labels
   * - key metas (description, custom) + tags
   * - key meta comments & code references
   * - key screenshot references
   * - translation comments
   * If sourceBranch is a default, keys with NULL branch_id are also treated as part of the source
   *
   * OPTIMIZATION: Uses a temporary key mapping table to avoid repeating the expensive
   * source-to-target key join across all 9 copy operations.
   */
  @Transactional
  override fun copy(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    require(sourceBranch.id != targetBranch.id) { "Source and target branch must differ" }

    // Step 1: Copy keys (must be first, creates target keys)
    traceLogMeasureTime("branchCopyService: copyKeys") {
      copyKeys(projectId, sourceBranch, targetBranch)
    }

    // Step 2: Create temporary key mapping table (computed once, reused 8 times)
    // NOTE: Must be created AFTER copyKeys() because it needs target keys to exist
    traceLogMeasureTime("branchCopyService: createKeyMapping") {
      createKeyMapping(projectId, sourceBranch, targetBranch)
    }

    // Step 3: Copy all related entities using the mapping
    traceLogMeasureTime("branchCopyService: copyTranslations") {
      copyTranslations(targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyTranslationLabels") {
      copyTranslationLabels()
    }

    traceLogMeasureTime("branchCopyService: copyKeyMetas") {
      copyKeyMetas(targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyKeyMetaTags") {
      copyKeyMetaTags()
    }

    traceLogMeasureTime("branchCopyService: copyKeyMetaComments") {
      copyKeyMetaComments(targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyKeyMetaCodeReferences") {
      copyKeyMetaCodeReferences(targetBranch)
    }

    traceLogMeasureTime("branchCopyService: copyKeyScreenshotReferences") {
      copyKeyScreenshotReferences()
    }

    traceLogMeasureTime("branchCopyService: copyTranslationComments") {
      copyTranslationComments(targetBranch)
    }
  }

  /**
   * Creates a temporary table mapping source key IDs to target key IDs.
   * This eliminates the need to repeat the expensive sk->tk join in every query.
   */
  private fun createKeyMapping(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    // Create temporary table with data in one step (more efficient)
    val createTableSql = """
      CREATE TEMPORARY TABLE temp_key_mapping
      ON COMMIT DROP
      AS
      SELECT sk.id AS source_key_id, tk.id AS target_key_id
      FROM key sk
      JOIN key tk ON tk.project_id = sk.project_id
                 AND tk.branch_id = :targetBranchId
                 AND tk.name = sk.name
                 AND tk.namespace_id IS NOT DISTINCT FROM sk.namespace_id
      WHERE sk.project_id = :projectId
        AND ${getSourceBranchFilter("sk", sourceBranch.isDefault)}
    """
    entityManager
      .createNativeQuery(createTableSql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()

    // Add primary key constraint after table creation
    val addPrimaryKeySql = "ALTER TABLE temp_key_mapping ADD PRIMARY KEY (source_key_id)"
    entityManager.createNativeQuery(addPrimaryKeySql).executeUpdate()

    // Create index on target_key_id for reverse lookups (used in some queries)
    val createIndexSql = "CREATE INDEX idx_temp_key_mapping_target ON temp_key_mapping (target_key_id)"
    entityManager.createNativeQuery(createIndexSql).executeUpdate()
  }

  private fun getSourceBranchFilter(
    keyAlias: String,
    sourceIsDefault: Boolean,
  ): String {
    return if (sourceIsDefault) {
      "($keyAlias.branch_id = :sourceBranchId OR $keyAlias.branch_id IS NULL)"
    } else {
      "$keyAlias.branch_id = :sourceBranchId"
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
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    val sql = """
      WITH source_keys AS (
        SELECT k.id AS old_id, k.name, k.namespace_id, k.is_plural, k.plural_arg_name
        FROM key k
        WHERE k.project_id = :projectId 
          AND ${getSourceBranchFilter("k", sourceBranch.isDefault)}
      ), existing_target AS (
        SELECT tk.id, tk.name, tk.namespace_id
        FROM key tk
        WHERE tk.project_id = :projectId 
          AND tk.branch_id = :targetBranchId
      ), to_insert AS (
        SELECT sk.* 
        FROM source_keys sk
        LEFT JOIN existing_target et 
          ON et.name = sk.name 
          AND et.namespace_id IS NOT DISTINCT FROM sk.namespace_id
        WHERE et.id IS NULL
      )
      INSERT INTO key (id, name, project_id, namespace_id, branch_id, is_plural, plural_arg_name, created_at, updated_at)
      SELECT nextval('hibernate_sequence') AS id,
             ti.name, :projectId, ti.namespace_id, :targetBranchId, 
             ti.is_plural, ti.plural_arg_name, 
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM to_insert ti
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("targetBranchId", targetBranch.id)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  /**
   * Copies translations using the pre-computed key mapping.
   * Much simpler and faster than the original 4-way join.
   */
  private fun copyTranslations(targetBranch: Branch) {
    val sql = """
      INSERT INTO translation (
        id, text, key_id, language_id, state, auto, mt_provider, 
        word_count, character_count, outdated, created_at, updated_at
      )
      SELECT nextval('hibernate_sequence') AS id,
             t.text, m.target_key_id, t.language_id, t.state, t.auto, t.mt_provider,
             t.word_count, t.character_count, t.outdated, 
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
