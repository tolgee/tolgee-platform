INSERT INTO project_contributor (project_id, user_id, first_contribution_at, last_contribution_at)
SELECT ar.project_id, ar.author_id, min(ar."timestamp"), max(ar."timestamp")
FROM activity_revision ar
WHERE ar.project_id IS NOT NULL AND ar.author_id IS NOT NULL
  AND (ar.type IS NOT NULL
       OR EXISTS (SELECT 1 FROM activity_modified_entity ame
                  WHERE ame.activity_revision_id = ar.id))
GROUP BY ar.project_id, ar.author_id
ON CONFLICT (project_id, user_id) DO UPDATE
    SET first_contribution_at =
            LEAST(project_contributor.first_contribution_at, EXCLUDED.first_contribution_at),
        last_contribution_at =
            GREATEST(project_contributor.last_contribution_at, EXCLUDED.last_contribution_at);
