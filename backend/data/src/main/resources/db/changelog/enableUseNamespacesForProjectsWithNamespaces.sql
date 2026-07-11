-- Namespaces are now disabled by default, but we want to enable them for projects which already use namespaces
-- not to disturb working workflows.
UPDATE project
SET use_namespaces = TRUE
WHERE project.id IN (
    SELECT project.id
    FROM project LEFT JOIN namespace ON project.id = namespace.project_id
    GROUP BY project.id
    HAVING COUNT(namespace.project_id) > 0
)