-- Backfill organization_usage_counter for every non-deleted organization.
--
-- Matches the rules of OrganizationStatsService.getKeyCount / getTranslationCount exactly:
--   key_count         = COUNT DISTINCT (project, name, namespace) of non-deleted keys in
--                       non-deleted projects, respecting the branching rule.
--   translation_count = COUNT DISTINCT (project, name, namespace, language) of non-empty
--                       translations whose key, project, language, and (if any) branch are
--                       not deleted, respecting the same branching rule.
--
-- One scan of `key` and one scan of `translation`, grouped by organization in a single pass
-- — no per-org LATERAL evaluation.
--
-- Idempotent via ON CONFLICT DO NOTHING — safe to re-run.

WITH org_key_counts AS (
    SELECT p.organization_owner_id AS org_id,
           count(*) AS cnt
    FROM (
        SELECT DISTINCT p.organization_owner_id, k.project_id, k.name, k.namespace_id
        FROM key k
        JOIN project p ON p.id = k.project_id AND p.deleted_at IS NULL
        LEFT JOIN branch b ON b.id = k.branch_id
        WHERE k.deleted_at IS NULL
          AND (k.branch_id IS NULL OR b.deleted_at IS NULL)
          AND (p.use_branching = TRUE OR k.branch_id IS NULL OR b.is_default = TRUE)
    ) p
    GROUP BY p.organization_owner_id
),
org_translation_counts AS (
    SELECT p.organization_owner_id AS org_id,
           count(*) AS cnt
    FROM (
        SELECT DISTINCT p.organization_owner_id, k.project_id, k.name, k.namespace_id, t.language_id
        FROM key k
        JOIN project p ON p.id = k.project_id AND p.deleted_at IS NULL
        LEFT JOIN branch b ON b.id = k.branch_id
        JOIN translation t ON t.key_id = k.id AND t.text IS NOT NULL AND t.text <> ''
        JOIN language l ON l.id = t.language_id AND l.deleted_at IS NULL
        WHERE k.deleted_at IS NULL
          AND (k.branch_id IS NULL OR b.deleted_at IS NULL)
          AND (p.use_branching = TRUE OR k.branch_id IS NULL OR b.is_default = TRUE)
    ) p
    GROUP BY p.organization_owner_id
)
INSERT INTO organization_usage_counter (
    organization_id,
    key_count,
    translation_count,
    last_reconciled_at,
    created_at,
    updated_at
)
SELECT o.id,
       COALESCE(kc.cnt, 0),
       COALESCE(tc.cnt, 0),
       NOW(),
       NOW(),
       NOW()
FROM organization o
LEFT JOIN org_key_counts kc ON kc.org_id = o.id
LEFT JOIN org_translation_counts tc ON tc.org_id = o.id
WHERE o.deleted_at IS NULL
ON CONFLICT (organization_id) DO NOTHING;
