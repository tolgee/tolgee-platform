CREATE OR REPLACE FUNCTION community_contribution_counts(rev_id bigint)
    RETURNS TABLE
            (
                user_id       bigint,
                language_id   bigint,
                contributions int,
                suggested     int,
                accepted      int,
                declined      int
            )
AS
$$
WITH revision AS (SELECT ar.author_id, ar.type
                  FROM activity_revision ar
                  WHERE ar.id = rev_id),
     touched AS (SELECT me.entity_class,
                        me.entity_id,
                        me.modifications -> 'state' ->> 'new'                          AS new_state,
                        (me.describing_relations -> 'language' ->> 'entityId')::bigint AS language_id
                 FROM activity_modified_entity me
                 WHERE me.activity_revision_id = rev_id
                   AND me.entity_class IN ('Translation', 'TranslationSuggestion', 'TranslationComment')),
     counted AS (SELECT r.author_id AS user_id, l.language_id, 1 AS contributions, 0 AS suggested, 0 AS accepted, 0 AS declined
                 FROM revision r,
                      (SELECT DISTINCT t.language_id FROM touched t) l
                 UNION ALL
                 SELECT r.author_id, t.language_id, 0, 1, 0, 0
                 FROM revision r,
                      touched t
                 WHERE r.type = 'CREATE_SUGGESTION'
                   AND t.entity_class = 'TranslationSuggestion'
                 UNION ALL
                 SELECT s.author_id, t.language_id, 0, 0, (t.new_state = 'ACCEPTED')::int, (t.new_state = 'DECLINED')::int
                 FROM touched t
                          JOIN translation_suggestion s ON s.id = t.entity_id
                 WHERE t.entity_class = 'TranslationSuggestion'
                   AND t.new_state IN ('ACCEPTED', 'DECLINED'))
SELECT c.user_id,
       c.language_id,
       sum(c.contributions)::int,
       sum(c.suggested)::int,
       sum(c.accepted)::int,
       sum(c.declined)::int
FROM counted c
WHERE c.user_id IS NOT NULL
  AND c.language_id IS NOT NULL
GROUP BY c.user_id, c.language_id
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION track_community_contribution_day()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.project_id IS NULL OR NEW.author_id IS NULL OR NOT is_translation_contribution(NEW.type) THEN
        RETURN NULL;
    END IF;

    -- ponytail: nothing is ever decremented (REVERSE_SUGGESTION, DELETE_SUGGESTION add contributions only).
    INSERT INTO community_contribution_day (project_id, user_id, language_id, day, contributions, suggested, accepted, declined)
    SELECT NEW.project_id, c.user_id, c.language_id, date(NEW."timestamp"), c.contributions, c.suggested, c.accepted, c.declined
    FROM community_contribution_counts(NEW.id) c
    ON CONFLICT (project_id, user_id, language_id, day) DO UPDATE
        SET contributions = community_contribution_day.contributions + EXCLUDED.contributions,
            suggested     = community_contribution_day.suggested + EXCLUDED.suggested,
            accepted      = community_contribution_day.accepted + EXCLUDED.accepted,
            declined      = community_contribution_day.declined + EXCLUDED.declined;

    RETURN NULL;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS track_community_contribution_day ON translation_suggestion;
DROP TRIGGER IF EXISTS track_community_contribution_day ON activity_revision;

-- Deferred like track_project_contributor: activity_modified_entity rows are inserted after the
-- revision and must exist when the counts run.
CREATE CONSTRAINT TRIGGER track_community_contribution_day
    AFTER INSERT
    ON activity_revision
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE PROCEDURE track_community_contribution_day();
