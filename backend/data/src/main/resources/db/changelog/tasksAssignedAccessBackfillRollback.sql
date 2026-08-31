-- TASKS_ASSIGNED_ACCESS is introduced by this release, so a binary from before it cannot read a scopes array
-- that still holds the name - Scope.valueOf throws on it. Rolling back therefore has to remove it again, from
-- every row that holds it and not only from the rows the backfill itself wrote: this release's UI and API can
-- grant the scope on its own, and a rollback happens exactly on a database the release has already run on.
--
-- A permission whose only scope was TASKS_ASSIGNED_ACCESS becomes the role-based NONE rather than an empty scope
-- array: Permission.PermissionListeners nulls an empty array and then demands exactly one of scopes or type, so
-- an emptied granular row cannot be saved again and the next edit of that member's permission would fail. NONE
-- grants nothing, which is what the row grants once the scope is gone; deleting it instead would fall back to
-- the organization base permission and could grant more than the user had.
update permission
set type   = 'NONE',
    scopes = null
where scopes = array ['TASKS_ASSIGNED_ACCESS']::varchar[];

update permission
set scopes = array_remove(scopes, 'TASKS_ASSIGNED_ACCESS')
where scopes is not null
  and 'TASKS_ASSIGNED_ACCESS' = any (scopes);

delete from api_key_scopes_enum where scopes_enum = 'TASKS_ASSIGNED_ACCESS';
