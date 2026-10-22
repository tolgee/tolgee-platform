CREATE OR REPLACE FUNCTION track_community_contribution_day()
    RETURNS TRIGGER AS
$$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO community_contribution_day (project_id, user_id, language_id, day, suggested, accepted, declined)
        VALUES (NEW.project_id, NEW.author_id, NEW.language_id, date(NEW.created_at), 1, 0, 0)
        ON CONFLICT (project_id, user_id, language_id, day) DO UPDATE
            SET suggested = community_contribution_day.suggested + 1;
    END IF;

    -- Hibernate rewrites every column on update, so a text edit also arrives here with state unchanged.
    IF (TG_OP = 'INSERT' OR OLD.state IS DISTINCT FROM NEW.state) AND NEW.state IN ('ACCEPTED', 'DECLINED') THEN
        -- ponytail: ACCEPTED -> ACTIVE -> DECLINED counts 1/1; nothing is ever decremented.
        INSERT INTO community_contribution_day (project_id, user_id, language_id, day, suggested, accepted, declined)
        VALUES (NEW.project_id, NEW.author_id, NEW.language_id, date(NEW.updated_at),
                0,
                CASE WHEN NEW.state = 'ACCEPTED' THEN 1 ELSE 0 END,
                CASE WHEN NEW.state = 'DECLINED' THEN 1 ELSE 0 END)
        ON CONFLICT (project_id, user_id, language_id, day) DO UPDATE
            SET accepted = community_contribution_day.accepted + EXCLUDED.accepted,
                declined = community_contribution_day.declined + EXCLUDED.declined;
    END IF;

    RETURN NULL;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS track_community_contribution_day ON translation_suggestion;

CREATE TRIGGER track_community_contribution_day
    AFTER INSERT OR UPDATE OF state
    ON translation_suggestion
    FOR EACH ROW
EXECUTE PROCEDURE track_community_contribution_day();
