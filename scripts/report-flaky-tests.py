#!/usr/bin/env python3
"""Report flaky tests to the 'Flaky tests' org project.

Reads failed-test lines from files in <failures-dir>, resolves each to an
owning developer via git blame (constrained to members of a GitHub team),
and creates or updates a draft issue on the org project.

Env vars:
  PROJECT_OWNER    org login (default: tolgee)
  PROJECT_NUMBER   project number (default: 4)
  TEAM_ORG         team org (default: tolgee)
  TEAM_SLUG        eligible-assignees team slug (default: coders)
  RUN_URL          CI run URL to record on the item
  GH_TOKEN         token with project:rw + org:members:read (required)

Usage:
  report-flaky-tests.py <failures-dir> [<search-root>...]

If search-root is omitted, '.' is used. Each failed test is resolved by
searching each root in order for a matching source file; git blame runs
in that root.
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from datetime import date
from pathlib import Path

PROJECT_OWNER = os.environ.get("PROJECT_OWNER", "tolgee")
PROJECT_NUMBER = int(os.environ.get("PROJECT_NUMBER", "4"))
TEAM_ORG = os.environ.get("TEAM_ORG", "tolgee")
TEAM_SLUG = os.environ.get("TEAM_SLUG", "coders")
RUN_URL = os.environ.get("RUN_URL", "")
MAX_ITEMS_PER_RUN = int(os.environ.get("MAX_ITEMS_PER_RUN", "30"))


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
            fh.write(f"{k}={v}\n")


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


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def read_failures(failures_dir: Path) -> list[str]:
    lines: set[str] = set()
    for f in sorted(failures_dir.glob("failed-tests-*.txt")):
        for raw in f.read_text(errors="replace").splitlines():
            line = raw.strip()
            if line:
                lines.add(line)
    return sorted(lines)


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    failures_dir = Path(sys.argv[1])
    roots = [Path(r) for r in sys.argv[2:]] or [Path(".")]

    if not failures_dir.is_dir():
        print(f"no failures dir: {failures_dir}", file=sys.stderr)
        return 0

    failures = read_failures(failures_dir)
    if not failures:
        print("no failed tests to report")
        return 0

    print(f"::group::flaky-test-tracker: {len(failures)} failed tests")
    for line in failures[:MAX_ITEMS_PER_RUN]:
        print(f"  - {line}")
    if len(failures) > MAX_ITEMS_PER_RUN:
        print(f"  (capped at {MAX_ITEMS_PER_RUN})")
    print("::endgroup::")

    if not os.environ.get("GH_TOKEN"):
        print("GH_TOKEN not set; skipping project update", file=sys.stderr)
        return 0

    project_id, project_url, fields = discover_project()
    write_output(project_url=project_url)
    required = ["Test", "Assignees", "Last run", "Fail count", "First seen"]
    missing = [n for n in required if n not in fields]
    if missing:
        print(f"project is missing fields: {missing}", file=sys.stderr)
        return 1

    eligible = team_members()
    if not eligible:
        print(
            f"team {TEAM_ORG}/{TEAM_SLUG} has no members readable with this "
            "token; cannot assign",
            file=sys.stderr,
        )
        return 1

    items = list_items(project_id)
    by_title = items_by_title(items)

    # Load per assignee (for balancing) based on open items
    load: dict[str, int] = {}
    for it in items:
        assignees = item_field(it, "Assignees") or []
        for login in assignees:
            load[login] = load.get(login, 0) + 1

    today = date.today().isoformat()
    new_count = recurring_count = 0

    for line in failures[:MAX_ITEMS_PER_RUN]:
        title = f"Flaky: {line}"
        existing = by_title.get(title)
        if existing:
            recurring_count += 1
            count = int(item_field(existing, "Fail count") or 0) + 1
            set_number(
                project_id, existing["id"], fields["Fail count"]["id"], count
            )
            set_text(
                project_id, existing["id"], fields["Last run"]["id"], RUN_URL
            )
            body = (existing.get("content") or {}).get("body") or ""
            body = body.rstrip() + f"\n- {today}: {RUN_URL}"
            try:
                update_draft_body(existing["id"], title, body)
            except RuntimeError as e:
                # Item may be a real Issue/PR, not a DraftIssue — skip body log
                print(f"  (skipped body update for {title}: {e})")
            print(f"  recurring ({count}x): {line}")
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
        set_text(project_id, item_id, fields["Test"]["id"], line)
        set_text(project_id, item_id, fields["Last run"]["id"], RUN_URL)
        set_number(project_id, item_id, fields["Fail count"]["id"], 1)
        set_date(project_id, item_id, fields["First seen"]["id"], today)
        print(f"  new -> @{owner}: {line}")

    write_output(new_count=new_count, recurring_count=recurring_count)
    print(f"done: {new_count} new, {recurring_count} recurring")
    return 0


if __name__ == "__main__":
    sys.exit(main())
