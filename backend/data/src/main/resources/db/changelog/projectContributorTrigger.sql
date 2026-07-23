CREATE OR REPLACE FUNCTION track_project_contributor()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.project_id IS NOT NULL AND NEW.author_id IS NOT NULL THEN
        INSERT INTO project_contributor (project_id, user_id, first_contribution_at, last_contribution_at)
        VALUES (NEW.project_id, NEW.author_id, NEW."timestamp", NEW."timestamp")
        ON CONFLICT (project_id, user_id) DO UPDATE
            SET first_contribution_at =
                    LEAST(project_contributor.first_contribution_at, EXCLUDED.first_contribution_at),
                last_contribution_at =
                    GREATEST(project_contributor.last_contribution_at, EXCLUDED.last_contribution_at);
    END IF;
    RETURN NEW;
END
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS track_project_contributor ON activity_revision;

CREATE TRIGGER track_project_contributor
    AFTER INSERT
    ON activity_revision
    FOR EACH ROW
EXECUTE PROCEDURE track_project_contributor();
