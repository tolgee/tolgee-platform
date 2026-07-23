CREATE OR REPLACE FUNCTION track_project_contributor()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.project_id IS NULL OR NEW.author_id IS NULL THEN
        RETURN NULL;
    END IF;

    INSERT INTO project_contributor (project_id, user_id, first_contribution_at, last_contribution_at)
    VALUES (NEW.project_id, NEW.author_id, NEW."timestamp", NEW."timestamp")
    ON CONFLICT (project_id, user_id) DO UPDATE
        SET first_contribution_at =
                LEAST(project_contributor.first_contribution_at, EXCLUDED.first_contribution_at),
            last_contribution_at =
                GREATEST(project_contributor.last_contribution_at, EXCLUDED.last_contribution_at)
        WHERE project_contributor.first_contribution_at > EXCLUDED.first_contribution_at
           OR project_contributor.last_contribution_at < EXCLUDED.last_contribution_at;

    RETURN NULL;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS track_project_contributor ON activity_revision;

CREATE CONSTRAINT TRIGGER track_project_contributor
    AFTER INSERT
    ON activity_revision
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE PROCEDURE track_project_contributor();
