-- Upserts recomputed counters for every (project, user, language, day) that still has a suggestion
-- behind it. A row whose suggestions were all deleted is not touched: to rebuild from scratch,
-- delete the rows first (CommunityStatsE2eDataController does), then run this.
INSERT INTO community_contribution_day (project_id, user_id, language_id, day, suggested, accepted, declined)
SELECT project_id, user_id, language_id, day, sum(suggested), sum(accepted), sum(declined)
FROM (SELECT project_id, author_id AS user_id, language_id, date(created_at) AS day, 1 AS suggested, 0 AS accepted, 0 AS declined
      FROM translation_suggestion
      UNION ALL
      SELECT project_id, author_id, language_id, date(updated_at), 0,
             CASE WHEN state = 'ACCEPTED' THEN 1 ELSE 0 END,
             CASE WHEN state = 'DECLINED' THEN 1 ELSE 0 END
      FROM translation_suggestion
      WHERE state IN ('ACCEPTED', 'DECLINED')) AS s
GROUP BY project_id, user_id, language_id, day
ON CONFLICT (project_id, user_id, language_id, day) DO UPDATE
    SET suggested = EXCLUDED.suggested,
        accepted = EXCLUDED.accepted,
        declined = EXCLUDED.declined;
