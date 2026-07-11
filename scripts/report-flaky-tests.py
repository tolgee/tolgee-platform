#!/usr/bin/env python3
"""Report flaky tests to the 'Flaky tests' org project.

Reads flaky-test lines from files in <flaky-dir>, resolves each to an
owning developer via git blame (constrained to members of a GitHub team),
and creates or updates a draft issue on the org project.

Each item carries a `Flakiness` status (Suspected / Confirmed). A test is
promoted to Confirmed only after it has been seen at least
`FLAKY_THRESHOLD` times within the last `FLAKY_WINDOW_DAYS` days, which
filters out one-off GitHub Actions infrastructure hiccups. Suspected items
that aren't promoted within `SUSPECTED_TTL_DAYS` days are deleted so the
board stays clean.

Env vars:
  PROJECT_OWNER       org login (default: tolgee)
  PROJECT_NUMBER      project number (default: 4)
  TEAM_ORG            team org (default: tolgee)
  TEAM_SLUG           eligible-assignees team slug (default: coders)
  RUN_URL             CI run URL to record on the item
  FLAKY_THRESHOLD     detections in the window required to promote
                      Suspected -> Confirmed (default: 3)
  FLAKY_WINDOW_DAYS   rolling-window length in days (default: 7)
  SUSPECTED_TTL_DAYS  delete Suspected items not seen for this many
                      days (default: 14)
  GH_TOKEN            token with project:rw + org:members:read (required)

Usage:
  report-flaky-tests.py <flaky-dir> [<search-root>...]

If search-root is omitted, '.' is used. Each flaky test is resolved by
searching each root in order for a matching source file; git blame runs
in that root.
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import tempfile
from datetime import date, timedelta
from pathlib import Path

PROJECT_OWNER = os.environ.get("PROJECT_OWNER", "tolgee")
PROJECT_NUMBER = int(os.environ.get("PROJECT_NUMBER", "4"))
TEAM_ORG = os.environ.get("TEAM_ORG", "tolgee")
TEAM_SLUG = os.environ.get("TEAM_SLUG", "coders")
RUN_URL = os.environ.get("RUN_URL", "")
MAX_ITEMS_PER_RUN = int(os.environ.get("MAX_ITEMS_PER_RUN", "30"))

# Confirmation gate: a test is promoted to Confirmed only after it has been
# seen at least FLAKY_THRESHOLD times within the last FLAKY_WINDOW_DAYS days.
FLAKY_THRESHOLD = int(os.environ.get("FLAKY_THRESHOLD", "3"))
FLAKY_WINDOW_DAYS = int(os.environ.get("FLAKY_WINDOW_DAYS", "7"))
SUSPECTED_TTL_DAYS = int(os.environ.get("SUSPECTED_TTL_DAYS", "14"))

FLAKINESS_FIELD = "Flakiness"
STATUS_SUSPECTED = "Suspected"
STATUS_CONFIRMED = "Confirmed"

# Run-log lines in the item body look like `- 2026-05-25: <run-url>`.
DATE_LINE_RE = re.compile(r"^- (\d{4}-\d{2}-\d{2}):", re.MULTILINE)


def run(cmd: list[str], **kwargs) -> str:
    res = subprocess.run(cmd, capture_output=True, text=True, **kwargs)
    if res.returncode != 0:
        raise RuntimeError(
            f"command failed: {' '.join(cmd)}\nstderr: {res.stderr}"
        )
    return res.stdout


def gh_json(*args: str) -> dict:
    return json.loads(run(["gh", "api", *args]))


def write_output(**kwargs) -> None:
    gho = os.environ.get("GITHUB_OUTPUT")
    if not gho:
        return
    with open(gho, "a") as fh:
        for k, v in kwargs.items():
            s = str(v)
            # GITHUB_OUTPUT requires the <<DELIM ... DELIM heredoc form for any
            # value containing newlines; use it for newly_confirmed_list etc.
            if "\n" in s:
                fh.write(f"{k}<<__OUTPUT_EOF__\n{s}\n__OUTPUT_EOF__\n")
            else:
                fh.write(f"{k}={s}\n")


def gql(query: str, **variables) -> dict:
    args = ["graphql", "-f", f"query={query}"]
    for k, v in variables.items():
        if v is None:
            continue
        flag = "-F" if isinstance(v, (int, bool)) else "-f"
        args += [flag, f"{k}={v}"]
    data = gh_json(*args)
    if "errors" in data:
        raise RuntimeError(f"graphql errors: {data['errors']}")
    return data["data"]


# ---------------------------------------------------------------------------
# Project metadata
# ---------------------------------------------------------------------------

def discover_project() -> tuple[str, str, dict]:
    q = """
    query($owner: String!, $number: Int!) {
      organization(login: $owner) {
        projectV2(number: $number) {
          id
          url
          fields(first: 30) {
            nodes {
              ... on ProjectV2Field { id name dataType }
              ... on ProjectV2SingleSelectField {
                id name dataType options { id name }
              }
            }
          }
        }
      }
    }
    """
    data = gql(q, owner=PROJECT_OWNER, number=PROJECT_NUMBER)
    p = data["organization"]["projectV2"]
    if not p:
        raise RuntimeError(f"project {PROJECT_OWNER}/{PROJECT_NUMBER} not found")
    fields = {n["name"]: n for n in p["fields"]["nodes"] if n}
    return p["id"], p["url"], fields


def list_items(project_id: str) -> list[dict]:
    items: list[dict] = []
    cursor: str | None = None
    q = """
    query($id: ID!, $cursor: String) {
      node(id: $id) {
        ... on ProjectV2 {
          items(first: 100, after: $cursor) {
            nodes {
              id
              content {
                __typename
                ... on DraftIssue { title body }
                ... on Issue { title }
                ... on PullRequest { title }
              }
              fieldValues(first: 30) {
                nodes {
                  ... on ProjectV2ItemFieldTextValue {
                    field { ... on ProjectV2FieldCommon { name } }
                    text
                  }
                  ... on ProjectV2ItemFieldNumberValue {
                    field { ... on ProjectV2FieldCommon { name } }
                    number
                  }
                  ... on ProjectV2ItemFieldDateValue {
                    field { ... on ProjectV2FieldCommon { name } }
                    date
                  }
                  ... on ProjectV2ItemFieldUserValue {
                    field { ... on ProjectV2FieldCommon { name } }
                    users(first: 10) { nodes { login } }
                  }
                  ... on ProjectV2ItemFieldSingleSelectValue {
                    field { ... on ProjectV2FieldCommon { name } }
                    optionId
                    name
                  }
                }
              }
            }
            pageInfo { hasNextPage endCursor }
          }
        }
      }
    }
    """
    while True:
        data = gql(q, id=project_id, cursor=cursor)
        page = data["node"]["items"]
        items.extend(page["nodes"])
        if not page["pageInfo"]["hasNextPage"]:
            break
        cursor = page["pageInfo"]["endCursor"]
    return items


def item_field(item: dict, name: str):
    for fv in item.get("fieldValues", {}).get("nodes", []) or []:
        f = fv.get("field") or {}
        if f.get("name") == name:
            for k in ("text", "number", "date"):
                if k in fv:
                    return fv[k]
            if "optionId" in fv:
                # Single-select: return the option's display name (e.g. "Confirmed").
                return fv.get("name")
            users = (fv.get("users") or {}).get("nodes")
            if users is not None:
                return [u["login"] for u in users]
    return None


def items_by_title(items: list[dict]) -> dict[str, dict]:
    out: dict[str, dict] = {}
    for it in items:
        content = it.get("content") or {}
        title = content.get("title")
        if title:
            out[title] = it
    return out


# ---------------------------------------------------------------------------
# Eligible assignees (GitHub team)
# ---------------------------------------------------------------------------

def team_members() -> list[str]:
    data = gh_json(f"/orgs/{TEAM_ORG}/teams/{TEAM_SLUG}/members", "--paginate")
    # --paginate may return concatenated JSON arrays; normalise
    if isinstance(data, list):
        return [u["login"] for u in data]
    return []


def user_by_email(email: str) -> str | None:
    try:
        res = gh_json(
            "/search/users",
            "-f", f"q={email} in:email",
        )
    except RuntimeError:
        return None
    for u in res.get("items", []):
        return u["login"]
    return None


# ---------------------------------------------------------------------------
# Test resolution & blame
# ---------------------------------------------------------------------------

CLASS_METHOD_RE = re.compile(r"^([A-Za-z0-9_.$]+)\.([A-Za-z0-9_$\[\]\- ]+)$")
CY_SPEC_RE = re.compile(r"^(.+?\.cy\.[tj]sx?)(?:/.*)?$")


def resolve_file(test_name: str, roots: list[Path]) -> tuple[Path, Path] | None:
    """Return (root, file_path) for the source file that owns the test.

    Supports two formats:
      - JUnit:   com.foo.BarTest.someMethod
      - Cypress: path/to/spec.cy.ts/My title > Inner
    """
    m = CY_SPEC_RE.match(test_name)
    if m:
        rel = m.group(1)
        for root in roots:
            for candidate in root.glob(f"**/{rel}"):
                if candidate.is_file():
                    return root, candidate
        return None

    m = CLASS_METHOD_RE.match(test_name)
    if m:
        fqcn = m.group(1)
        simple = fqcn.rsplit(".", 1)[-1]
        package_parts = fqcn.rsplit(".", 1)[0].split(".") if "." in fqcn else []
        for root in roots:
            candidates = list(root.glob(f"**/{simple}.kt")) + \
                         list(root.glob(f"**/{simple}.java"))
            # Prefer candidates whose path contains more package parts
            def score(p: Path) -> int:
                s = str(p).replace(os.sep, "/")
                return sum(1 for part in package_parts if f"/{part}/" in f"/{s}/")
            candidates.sort(key=score, reverse=True)
            if candidates:
                return root, candidates[0]
    return None


def blame_email(root: Path, file: Path) -> str | None:
    try:
        out = run(
            ["git", "-C", str(root), "blame", "--line-porcelain", "--",
             str(file.relative_to(root))]
        )
    except RuntimeError:
        return None
    emails: dict[str, int] = {}
    for match in re.finditer(r"^author-mail <([^>]+)>$", out, re.MULTILINE):
        email = match.group(1)
        emails[email] = emails.get(email, 0) + 1
    if not emails:
        return None
    return sorted(emails, key=lambda e: (-emails[e], e))[0]


# ---------------------------------------------------------------------------
# Owner selection
# ---------------------------------------------------------------------------

_email_to_login: dict[str, str | None] = {}


def email_to_login(email: str) -> str | None:
    if email in _email_to_login:
        return _email_to_login[email]
    # Try the GitHub no-reply convention first: <id>+<login>@users.noreply.github.com
    m = re.match(r"(?:\d+\+)?([^@]+)@users\.noreply\.github\.com$", email)
    if m:
        _email_to_login[email] = m.group(1)
        return m.group(1)
    login = user_by_email(email)
    _email_to_login[email] = login
    return login


def choose_owner(
    blame_login: str | None,
    eligible: list[str],
    load: dict[str, int],
) -> str:
    if blame_login and blame_login in eligible:
        return blame_login
    # Least-loaded among eligible, ties broken alphabetically
    return sorted(eligible, key=lambda u: (load.get(u, 0), u))[0]


# ---------------------------------------------------------------------------
# Mutations
# ---------------------------------------------------------------------------

def create_draft(
    project_id: str, title: str, body: str, assignee_ids: list[str]
) -> str:
    import tempfile

    q = (
        "mutation($project:ID!,$title:String!,$body:String!,$assignees:[ID!]){"
        " addProjectV2DraftIssue(input:{"
        " projectId:$project, title:$title, body:$body, assigneeIds:$assignees"
        " }) { projectItem { id } } }"
    )
    variables = {
        "project": project_id,
        "title": title,
        "body": body,
        "assignees": assignee_ids,
    }
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as fh:
        json.dump({"query": q, "variables": variables}, fh)
        path = fh.name
    try:
        resp = json.loads(run(["gh", "api", "graphql", "--input", path]))
        if resp.get("errors"):
            raise RuntimeError(f"create_draft errors: {resp['errors']}")
        return resp["data"]["addProjectV2DraftIssue"]["projectItem"]["id"]
    finally:
        try:
            os.unlink(path)
        except OSError:
            pass


def set_text(project_id: str, item_id: str, field_id: str, value: str) -> None:
    q = """
    mutation($p:ID!,$i:ID!,$f:ID!,$v:String!) {
      updateProjectV2ItemFieldValue(input:{
        projectId:$p, itemId:$i, fieldId:$f, value:{text:$v}
      }) { projectV2Item { id } }
    }
    """
    gql(q, p=project_id, i=item_id, f=field_id, v=value)


def set_number(project_id: str, item_id: str, field_id: str, value: int) -> None:
    q = """
    mutation($p:ID!,$i:ID!,$f:ID!,$v:Float!) {
      updateProjectV2ItemFieldValue(input:{
        projectId:$p, itemId:$i, fieldId:$f, value:{number:$v}
      }) { projectV2Item { id } }
    }
    """
    gql(q, p=project_id, i=item_id, f=field_id, v=value)


def set_date(project_id: str, item_id: str, field_id: str, value: str) -> None:
    q = """
    mutation($p:ID!,$i:ID!,$f:ID!,$v:Date!) {
      updateProjectV2ItemFieldValue(input:{
        projectId:$p, itemId:$i, fieldId:$f, value:{date:$v}
      }) { projectV2Item { id } }
    }
    """
    gql(q, p=project_id, i=item_id, f=field_id, v=value)


_login_to_node: dict[str, str | None] = {}


def user_node_id(login: str) -> str | None:
    if login in _login_to_node:
        return _login_to_node[login]
    try:
        data = gh_json(f"/users/{login}")
        node_id = data.get("node_id")
    except RuntimeError:
        node_id = None
    _login_to_node[login] = node_id
    return node_id


def update_draft_body(item_id: str, title: str, body: str) -> None:
    q = """
    mutation($i:ID!,$t:String!,$b:String!) {
      updateProjectV2DraftIssue(input:{
        draftIssueId:$i, title:$t, body:$b
      }) { draftIssue { id } }
    }
    """
    # DraftIssue node id != ProjectV2Item id. Need to fetch the draft issue id.
    q_fetch = """
    query($id: ID!) {
      node(id: $id) {
        ... on ProjectV2Item {
          content { ... on DraftIssue { id } }
        }
      }
    }
    """
    data = gql(q_fetch, id=item_id)
    content = (data.get("node") or {}).get("content") or {}
    draft_id = content.get("id")
    if not draft_id:
        raise RuntimeError("project item content is not a DraftIssue")
    gql(q, i=draft_id, t=title, b=body)


def set_single_select(
    project_id: str, item_id: str, field_id: str, option_id: str
) -> None:
    q = """
    mutation($p:ID!,$i:ID!,$f:ID!,$v:String!) {
      updateProjectV2ItemFieldValue(input:{
        projectId:$p, itemId:$i, fieldId:$f, value:{singleSelectOptionId:$v}
      }) { projectV2Item { id } }
    }
    """
    gql(q, p=project_id, i=item_id, f=field_id, v=option_id)


def delete_item(project_id: str, item_id: str) -> None:
    q = """
    mutation($p:ID!,$i:ID!) {
      deleteProjectV2Item(input:{projectId:$p, itemId:$i}) { deletedItemId }
    }
    """
    gql(q, p=project_id, i=item_id)


def gql_with_input(query: str, variables: dict) -> dict:
    """Run a GraphQL operation that takes list/object variables (`gh` CLI's
    `-f`/`-F` flags only handle scalars)."""
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as fh:
        json.dump({"query": query, "variables": variables}, fh)
        path = fh.name
    try:
        resp = json.loads(run(["gh", "api", "graphql", "--input", path]))
        if resp.get("errors"):
            raise RuntimeError(f"graphql errors: {resp['errors']}")
        return resp["data"]
    finally:
        try:
            os.unlink(path)
        except OSError:
            pass


def ensure_flakiness_field(project_id: str, fields: dict) -> dict:
    """Create the Flakiness single-select field if it doesn't exist yet.

    Returns the (possibly refreshed) fields dict.
    """
    if FLAKINESS_FIELD in fields:
        return fields
    q = """
    mutation($project:ID!,$name:String!,$opts:[ProjectV2SingleSelectFieldOptionInput!]!) {
      createProjectV2Field(input:{
        projectId:$project, dataType:SINGLE_SELECT, name:$name,
        singleSelectOptions:$opts
      }) { projectV2Field { ... on ProjectV2SingleSelectField { id name } } }
    }
    """
    opts = [
        {
            "name": STATUS_SUSPECTED,
            "color": "GRAY",
            "description": (
                f"Detected fewer than {FLAKY_THRESHOLD} times in the past "
                f"{FLAKY_WINDOW_DAYS} days"
            ),
        },
        {
            "name": STATUS_CONFIRMED,
            "color": "RED",
            "description": (
                f"Detected at least {FLAKY_THRESHOLD} times in the past "
                f"{FLAKY_WINDOW_DAYS} days"
            ),
        },
    ]
    gql_with_input(
        q, {"project": project_id, "name": FLAKINESS_FIELD, "opts": opts}
    )
    print(f"created project field '{FLAKINESS_FIELD}' with options "
          f"{STATUS_SUSPECTED}, {STATUS_CONFIRMED}")
    # Re-fetch to pick up the new field and its option IDs.
    return discover_project()[2]


# ---------------------------------------------------------------------------
# Confirmation gate (rolling-window classification)
# ---------------------------------------------------------------------------

def parse_body_dates(body: str) -> list[date]:
    """Extract dates from `- YYYY-MM-DD:` lines in the item body."""
    out: list[date] = []
    for m in DATE_LINE_RE.finditer(body or ""):
        try:
            out.append(date.fromisoformat(m.group(1)))
        except ValueError:
            pass
    return out


def classify(dates: list[date]) -> str:
    """Return Confirmed if `dates` has FLAKY_THRESHOLD entries within the
    rolling window, otherwise Suspected."""
    cutoff = date.today() - timedelta(days=FLAKY_WINDOW_DAYS)
    recent = sum(1 for d in dates if d >= cutoff)
    return STATUS_CONFIRMED if recent >= FLAKY_THRESHOLD else STATUS_SUSPECTED


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def read_flaky_tests(flaky_dir: Path) -> list[str]:
    lines: set[str] = set()
    for f in sorted(flaky_dir.glob("flaky-tests-*.txt")):
        for raw in f.read_text(errors="replace").splitlines():
            line = raw.strip()
            if line:
                lines.add(line)
    return sorted(lines)


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    flaky_dir = Path(sys.argv[1])
    roots = [Path(r) for r in sys.argv[2:]] or [Path(".")]

    # An empty/missing flaky dir is fine — the sweep pass still runs so
    # stale Suspected items get cleaned up.
    flaky_tests = read_flaky_tests(flaky_dir) if flaky_dir.is_dir() else []

    print(f"::group::flaky-test-tracker: {len(flaky_tests)} flaky tests")
    for line in flaky_tests[:MAX_ITEMS_PER_RUN]:
        print(f"  - {line}")
    if len(flaky_tests) > MAX_ITEMS_PER_RUN:
        print(f"  (capped at {MAX_ITEMS_PER_RUN})")
    print("::endgroup::")

    if not os.environ.get("GH_TOKEN"):
        print("GH_TOKEN not set; skipping project update", file=sys.stderr)
        return 0

    project_id, project_url, fields = discover_project()
    fields = ensure_flakiness_field(project_id, fields)
    write_output(project_url=project_url)
    required = [
        "Test", "Assignees", "Last run", "Fail count", "First seen",
        FLAKINESS_FIELD,
    ]
    missing = [n for n in required if n not in fields]
    if missing:
        print(f"project is missing fields: {missing}", file=sys.stderr)
        return 1

    flakiness_options = {
        o["name"]: o["id"]
        for o in fields[FLAKINESS_FIELD].get("options", [])
    }
    for required_opt in (STATUS_SUSPECTED, STATUS_CONFIRMED):
        if required_opt not in flakiness_options:
            print(
                f"{FLAKINESS_FIELD} field is missing required option: "
                f"{required_opt}",
                file=sys.stderr,
            )
            return 1
    suspected_opt = flakiness_options[STATUS_SUSPECTED]
    confirmed_opt = flakiness_options[STATUS_CONFIRMED]

    items = list_items(project_id)
    by_title = items_by_title(items)

    if flaky_tests:
        eligible = team_members()
        if not eligible:
            print(
                f"team {TEAM_ORG}/{TEAM_SLUG} has no members readable with "
                "this token; cannot assign",
                file=sys.stderr,
            )
            return 1
        # Load per assignee (for balancing) based on open items
        load: dict[str, int] = {}
        for it in items:
            assignees = item_field(it, "Assignees") or []
            for login in assignees:
                load[login] = load.get(login, 0) + 1
    else:
        eligible = []
        load = {}

    today_d = date.today()
    today = today_d.isoformat()
    new_count = recurring_count = newly_confirmed_count = 0
    newly_confirmed_ids: list[str] = []
    touched_ids: set[str] = set()

    for line in flaky_tests[:MAX_ITEMS_PER_RUN]:
        title = f"Flaky: {line}"
        existing = by_title.get(title)
        if existing:
            recurring_count += 1
            touched_ids.add(existing["id"])
            count = int(item_field(existing, "Fail count") or 0) + 1
            set_number(
                project_id, existing["id"], fields["Fail count"]["id"], count
            )
            set_text(
                project_id, existing["id"], fields["Last run"]["id"], RUN_URL
            )
            body = (existing.get("content") or {}).get("body") or ""
            new_body = body.rstrip() + f"\n- {today}: {RUN_URL}"
            try:
                update_draft_body(existing["id"], title, new_body)
            except RuntimeError as e:
                # Item may be a real Issue/PR, not a DraftIssue — skip body log
                print(f"  (skipped body update for {title}: {e})")
            # Always classify against today's detection, even if the body
            # update didn't persist — the status reflects what we observed.
            new_status = classify(parse_body_dates(new_body))
            old_status = item_field(existing, FLAKINESS_FIELD)
            if new_status != old_status:
                set_single_select(
                    project_id, existing["id"],
                    fields[FLAKINESS_FIELD]["id"],
                    confirmed_opt if new_status == STATUS_CONFIRMED
                    else suspected_opt,
                )
            if (
                old_status != STATUS_CONFIRMED
                and new_status == STATUS_CONFIRMED
            ):
                newly_confirmed_count += 1
                newly_confirmed_ids.append(line)
                print(f"  promoted -> Confirmed ({count}x total): {line}")
            else:
                print(f"  recurring ({count}x, {new_status}): {line}")
            continue

        new_count += 1
        resolved = resolve_file(line, roots)
        blame_login: str | None = None
        if resolved:
            root, file = resolved
            email = blame_email(root, file)
            if email:
                blame_login = email_to_login(email)

        owner = choose_owner(blame_login, eligible, load)
        load[owner] = load.get(owner, 0) + 1

        suffix = ""
        if blame_login != owner:
            who = f"@{blame_login}" if blame_login else "unknown"
            suffix = f" (rotation — last git-blame author: {who})"
        body = "\n".join(
            [
                f"**Test:** `{line}`",
                f"**Owner:** @{owner}{suffix}",
                "",
                "**Run log:**",
                f"- {today}: {RUN_URL}",
            ]
        )
        node_id = user_node_id(owner)
        assignees = [node_id] if node_id else []
        if not node_id:
            print(f"  (could not resolve node id for @{owner})")
        item_id = create_draft(project_id, title, body, assignees)
        touched_ids.add(item_id)
        set_text(project_id, item_id, fields["Test"]["id"], line)
        set_text(project_id, item_id, fields["Last run"]["id"], RUN_URL)
        set_number(project_id, item_id, fields["Fail count"]["id"], 1)
        set_date(project_id, item_id, fields["First seen"]["id"], today)
        # First detection is normally Suspected; only Confirmed if threshold==1.
        initial_status = classify([today_d])
        set_single_select(
            project_id, item_id, fields[FLAKINESS_FIELD]["id"],
            confirmed_opt if initial_status == STATUS_CONFIRMED else suspected_opt,
        )
        if initial_status == STATUS_CONFIRMED:
            newly_confirmed_count += 1
            newly_confirmed_ids.append(line)
        print(f"  new -> @{owner} ({initial_status}): {line}")

    # Sweep: delete Suspected items whose most recent detection is older
    # than SUSPECTED_TTL_DAYS — they were one-off hiccups that never recurred.
    deleted_suspect_count = 0
    suspected_cutoff = today_d - timedelta(days=SUSPECTED_TTL_DAYS)
    for it in items:
        if it["id"] in touched_ids:
            continue
        if item_field(it, FLAKINESS_FIELD) != STATUS_SUSPECTED:
            continue
        body = (it.get("content") or {}).get("body") or ""
        dates = parse_body_dates(body)
        if not dates or max(dates) >= suspected_cutoff:
            continue
        title = (it.get("content") or {}).get("title") or "?"
        try:
            delete_item(project_id, it["id"])
            deleted_suspect_count += 1
            print(f"  deleted stale suspected item: {title}")
        except RuntimeError as e:
            print(f"  (failed to delete {title}: {e})")

    # Re-fetch items so the totals reflect this run's writes (newly-created
    # items, promotions, swept deletions) — the pre-run `items` snapshot
    # doesn't include any of them, and on the very first run no item had a
    # Flakiness value at fetch time.
    items_after = list_items(project_id)
    total_confirmed = sum(
        1 for it in items_after
        if item_field(it, FLAKINESS_FIELD) == STATUS_CONFIRMED
    )
    total_suspected = sum(
        1 for it in items_after
        if item_field(it, FLAKINESS_FIELD) == STATUS_SUSPECTED
    )

    newly_confirmed_list = "\n".join(
        f"• `{n}`" for n in newly_confirmed_ids[:30]
    )

    write_output(
        new_count=new_count,
        recurring_count=recurring_count,
        newly_confirmed_count=newly_confirmed_count,
        newly_confirmed_list=newly_confirmed_list,
        deleted_suspect_count=deleted_suspect_count,
        total_confirmed_count=total_confirmed,
        total_suspected_count=total_suspected,
    )
    print(
        f"done: {new_count} new, {recurring_count} recurring, "
        f"{newly_confirmed_count} newly confirmed, "
        f"{deleted_suspect_count} stale suspected removed"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
