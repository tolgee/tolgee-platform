-- Twin of community_contribution_counts; CommunityContributionDayTest pins the two to the same result.
DELETE FROM community_contribution_day;

INSERT INTO community_contribution_day (project_id, user_id, language_id, day, contributions, suggested, accepted, declined)
WITH touched AS (SELECT ar.id                                   AS rev_id,
                        ar.project_id,
                        ar.author_id,
                        ar.type,
                        date(ar."timestamp")                    AS day,
                        me.entity_class,
                        me.entity_id,
                        me.modifications -> 'state' ->> 'new'                          AS new_state,
                        (me.describing_relations -> 'language' ->> 'entityId')::bigint AS language_id
                 FROM activity_revision ar
                          JOIN activity_modified_entity me
                               ON me.activity_revision_id = ar.id
                                   AND me.entity_class IN ('Translation', 'TranslationSuggestion', 'TranslationComment')
                 WHERE ar.project_id IS NOT NULL
                   AND ar.author_id IS NOT NULL
                   AND is_translation_contribution(ar.type)),
     counted AS (SELECT l.project_id, l.author_id AS user_id, l.language_id, l.day, 1 AS contributions, 0 AS suggested, 0 AS accepted, 0 AS declined
                 FROM (SELECT DISTINCT t.rev_id, t.project_id, t.author_id, t.language_id, t.day FROM touched t) l
                 UNION ALL
                 SELECT t.project_id, t.author_id, t.language_id, t.day, 0, 1, 0, 0
                 FROM touched t
                 WHERE t.type = 'CREATE_SUGGESTION'
                   AND t.entity_class = 'TranslationSuggestion'
                 UNION ALL
                 -- ponytail: suggester from the live row, so accepted/declined of suggestions deleted since are dropped;
                 -- resolve it from the CREATE_SUGGESTION revision if that matters.
                 SELECT t.project_id, s.author_id, t.language_id, t.day, 0, 0, (t.new_state = 'ACCEPTED')::int, (t.new_state = 'DECLINED')::int
                 FROM touched t
                          JOIN translation_suggestion s ON s.id = t.entity_id
                 WHERE t.entity_class = 'TranslationSuggestion'
                   AND t.new_state IN ('ACCEPTED', 'DECLINED'))
SELECT c.project_id, c.user_id, c.language_id, c.day,
       sum(c.contributions), sum(c.suggested), sum(c.accepted), sum(c.declined)
FROM counted c
WHERE c.user_id IS NOT NULL
  AND c.language_id IS NOT NULL
GROUP BY c.project_id, c.user_id, c.language_id, c.day
-- A revision committed by a still-running old instance while this runs has already fired the trigger.
ON CONFLICT (project_id, user_id, language_id, day) DO UPDATE
    SET contributions = community_contribution_day.contributions + EXCLUDED.contributions,
        suggested     = community_contribution_day.suggested + EXCLUDED.suggested,
        accepted      = community_contribution_day.accepted + EXCLUDED.accepted,
        declined      = community_contribution_day.declined + EXCLUDED.declined;
