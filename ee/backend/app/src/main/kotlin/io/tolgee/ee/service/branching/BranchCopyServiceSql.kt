package io.tolgee.ee.service.branching

import io.tolgee.model.branching.Branch
import io.tolgee.service.branching.BranchCopyService
import io.tolgee.util.Logging
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    // Step 4: Create translation mapping table (computed once, reused for labels and comments)
    // NOTE: Must be created AFTER copyTranslations() because it needs target translations to exist
    traceLogMeasureTime("branchCopyService: createTranslationMapping") {
      createTranslationMapping()
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
                 AND coalesce(tk.namespace_id,-1) = coalesce(sk.namespace_id,-1)
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

  /**
   * Creates a temporary table mapping source translation IDs to target translation IDs.
   * This eliminates the need to repeat the expensive 4-way join in translation label and comment copying.
   */
  private fun createTranslationMapping() {
    val createTableSql = """
      CREATE TEMPORARY TABLE temp_translation_mapping
      ON COMMIT DROP
      AS
      SELECT src_t.id AS source_translation_id, tgt_t.id AS target_translation_id
      FROM translation src_t
      JOIN temp_key_mapping m ON m.source_key_id = src_t.key_id
      JOIN translation tgt_t ON tgt_t.key_id = m.target_key_id
                            AND tgt_t.language_id = src_t.language_id
    """
    entityManager.createNativeQuery(createTableSql).executeUpdate()

    // Add primary key for efficient lookups
    val addPrimaryKeySql = "ALTER TABLE temp_translation_mapping ADD PRIMARY KEY (source_translation_id)"
    entityManager.createNativeQuery(addPrimaryKeySql).executeUpdate()
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
   * Uses batch sequence allocation for performance.
   */
  private fun copyKeyMetas(targetBranch: Branch) {
    val sql = """
      WITH to_insert AS (
        SELECT m.target_key_id, km.description, km.custom,
               row_number() OVER (ORDER BY km.id) AS rn
        FROM key_meta km
        JOIN temp_key_mapping m ON m.source_key_id = km.key_id
      ), id_range AS (
        SELECT setval('hibernate_sequence', nextval('hibernate_sequence') + cnt - 1) - cnt + 1 AS start_id
        FROM (SELECT count(*) AS cnt FROM to_insert) c
        WHERE cnt > 0
      )
      INSERT INTO key_meta (id, key_id, description, custom, created_at, updated_at)
      SELECT (SELECT start_id FROM id_range) + ti.rn - 1,
             ti.target_key_id, ti.description, ti.custom,
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM to_insert ti
      WHERE EXISTS (SELECT 1 FROM id_range)
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
   * Uses batch sequence allocation for performance.
   */
  private fun copyKeyMetaComments(targetBranch: Branch) {
    val sql = """
      WITH to_insert AS (
        SELECT tkm.id AS target_key_meta_id, kc.author_id, kc.text, kc.from_import,
               row_number() OVER (ORDER BY kc.id) AS rn
        FROM key_comment kc
        JOIN key_meta km ON km.id = kc.key_meta_id
        JOIN temp_key_mapping m ON m.source_key_id = km.key_id
        JOIN key_meta tkm ON tkm.key_id = m.target_key_id
      ), id_range AS (
        SELECT setval('hibernate_sequence', nextval('hibernate_sequence') + cnt - 1) - cnt + 1 AS start_id
        FROM (SELECT count(*) AS cnt FROM to_insert) c
        WHERE cnt > 0
      )
      INSERT INTO key_comment (id, key_meta_id, author_id, text, from_import, created_at, updated_at)
      SELECT (SELECT start_id FROM id_range) + ti.rn - 1,
             ti.target_key_meta_id, ti.author_id, ti.text, ti.from_import,
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM to_insert ti
      WHERE EXISTS (SELECT 1 FROM id_range)
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  /**
   * Copies key meta code references using the pre-computed key mapping.
   * Uses batch sequence allocation for performance.
   */
  private fun copyKeyMetaCodeReferences(targetBranch: Branch) {
    val sql = """
      WITH to_insert AS (
        SELECT tkm.id AS target_key_meta_id, kcr.author_id, kcr.path, kcr.line, kcr.from_import,
               row_number() OVER (ORDER BY kcr.id) AS rn
        FROM key_code_reference kcr
        JOIN key_meta km ON km.id = kcr.key_meta_id
        JOIN temp_key_mapping m ON m.source_key_id = km.key_id
        JOIN key_meta tkm ON tkm.key_id = m.target_key_id
      ), id_range AS (
        SELECT setval('hibernate_sequence', nextval('hibernate_sequence') + cnt - 1) - cnt + 1 AS start_id
        FROM (SELECT count(*) AS cnt FROM to_insert) c
        WHERE cnt > 0
      )
      INSERT INTO key_code_reference (id, key_meta_id, author_id, path, line, from_import, created_at, updated_at)
      SELECT (SELECT start_id FROM id_range) + ti.rn - 1,
             ti.target_key_meta_id, ti.author_id, ti.path, ti.line, ti.from_import,
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM to_insert ti
      WHERE EXISTS (SELECT 1 FROM id_range)
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
   * Copies translation comments using the pre-computed translation mapping.
   * Uses batch sequence allocation for performance.
   */
  private fun copyTranslationComments(targetBranch: Branch) {
    val sql = """
      WITH to_insert AS (
        SELECT tc.text, tc.state, tm.target_translation_id, tc.author_id,
               row_number() OVER (ORDER BY tc.id) AS rn
        FROM translation_comment tc
        JOIN temp_translation_mapping tm ON tm.source_translation_id = tc.translation_id
      ), id_range AS (
        SELECT setval('hibernate_sequence', nextval('hibernate_sequence') + cnt - 1) - cnt + 1 AS start_id
        FROM (SELECT count(*) AS cnt FROM to_insert) c
        WHERE cnt > 0
      )
      INSERT INTO translation_comment (id, text, state, translation_id, author_id, created_at, updated_at)
      SELECT (SELECT start_id FROM id_range) + ti.rn - 1,
             ti.text, ti.state, ti.target_translation_id, ti.author_id,
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM to_insert ti
      WHERE EXISTS (SELECT 1 FROM id_range)
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
    // Uses batch sequence allocation: pre-increment sequence once, then compute IDs with row_number()
    // This eliminates per-row nextval() calls which is a major performance bottleneck
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
        SELECT sk.*, row_number() OVER (ORDER BY sk.old_id) AS rn
        FROM source_keys sk
        LEFT JOIN existing_target et
          ON et.name = sk.name
          AND coalesce(et.namespace_id,-1) = coalesce(sk.namespace_id,-1)
        WHERE et.id IS NULL
      ), id_range AS (
        SELECT setval('hibernate_sequence', nextval('hibernate_sequence') + cnt - 1) - cnt + 1 AS start_id
        FROM (SELECT count(*) AS cnt FROM to_insert) c
        WHERE cnt > 0
      )
      INSERT INTO key (id, name, project_id, namespace_id, branch_id, is_plural, plural_arg_name, created_at, updated_at)
      SELECT (SELECT start_id FROM id_range) + ti.rn - 1,
             ti.name, :projectId, ti.namespace_id, :targetBranchId,
             ti.is_plural, ti.plural_arg_name,
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM to_insert ti
      WHERE EXISTS (SELECT 1 FROM id_range)
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
   * Uses batch sequence allocation for performance.
   */
  private fun copyTranslations(targetBranch: Branch) {
    val sql = """
      WITH to_insert AS (
        SELECT t.text, m.target_key_id, t.language_id, t.state, t.auto, t.mt_provider,
               t.word_count, t.character_count, t.outdated,
               row_number() OVER (ORDER BY t.id) AS rn
        FROM translation t
        JOIN temp_key_mapping m ON m.source_key_id = t.key_id
      ), id_range AS (
        SELECT setval('hibernate_sequence', nextval('hibernate_sequence') + cnt - 1) - cnt + 1 AS start_id
        FROM (SELECT count(*) AS cnt FROM to_insert) c
        WHERE cnt > 0
      )
      INSERT INTO translation (
        id, text, key_id, language_id, state, auto, mt_provider,
        word_count, character_count, outdated, created_at, updated_at
      )
      SELECT (SELECT start_id FROM id_range) + ti.rn - 1,
             ti.text, ti.target_key_id, ti.language_id, ti.state, ti.auto, ti.mt_provider,
             ti.word_count, ti.character_count, ti.outdated,
             :targetBranchCreatedAt, :targetBranchCreatedAt
      FROM to_insert ti
      WHERE EXISTS (SELECT 1 FROM id_range)
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("targetBranchCreatedAt", targetBranch.createdAt)
      .executeUpdate()
  }

  /**
   * Copies translation labels using the pre-computed translation mapping.
   */
  private fun copyTranslationLabels() {
    val sql = """
      INSERT INTO translation_label (translation_id, label_id)
      SELECT tm.target_translation_id, tl.label_id
      FROM translation_label tl
      JOIN temp_translation_mapping tm ON tm.source_translation_id = tl.translation_id
    """
    entityManager.createNativeQuery(sql).executeUpdate()
  }
}
