-- Before tasks.assigned-access existed, being a task's assignee was enough to view and edit it, whatever the
-- permission said. The scope now gates that fallback, and role-based permissions pick it up because
-- ProjectPermissionType expands at runtime — but granular permissions and API keys store their scope list
-- literally, so without this backfill every existing one silently loses the elevation on upgrade.
--
-- Only rows that already grant something are touched: access is gated on the scope set being non-empty
-- (ProjectContextService hides the project entirely when it is), so adding a scope to a permission that
-- deliberately grants nothing would turn it into project visibility.
update permission
set scopes = array_append(scopes, 'TASKS_ASSIGNED_ACCESS')
where scopes is not null
  and cardinality(scopes) > 0
  and not ('TASKS_ASSIGNED_ACCESS' = any (scopes));

insert into api_key_scopes_enum (api_key_id, scopes_enum)
select k.id, 'TASKS_ASSIGNED_ACCESS'
from api_key k
where exists (select 1 from api_key_scopes_enum s where s.api_key_id = k.id)
  and not exists (select 1
                  from api_key_scopes_enum s
                  where s.api_key_id = k.id
                    and s.scopes_enum = 'TASKS_ASSIGNED_ACCESS');
