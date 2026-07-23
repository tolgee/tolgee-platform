-- Insert-and-widen only. To rebuild a project_contributor suspected of drift, delete the affected
-- rows and re-run this statement; it is idempotent and derives everything from the activity log.
INSERT INTO project_contributor (project_id, user_id, first_contribution_at, last_contribution_at)
SELECT ar.project_id, ar.author_id, min(ar."timestamp"), max(ar."timestamp")
FROM activity_revision ar
WHERE ar.project_id IS NOT NULL AND ar.author_id IS NOT NULL
GROUP BY ar.project_id, ar.author_id
ON CONFLICT (project_id, user_id) DO UPDATE
    SET first_contribution_at =
            LEAST(project_contributor.first_contribution_at, EXCLUDED.first_contribution_at),
        last_contribution_at =
            GREATEST(project_contributor.last_contribution_at, EXCLUDED.last_contribution_at);
