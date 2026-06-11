# Snyk Code (SAST) triage

Disposition record for the Snyk **Code** findings (static analysis of source, as
opposed to Snyk Open Source, which scans dependencies). Each entry below has been
reviewed against the actual code.

Scanned with `snyk code test` (18 findings: 10 high, 2 medium, 6 low). After
review, **none represent an exploitable vulnerability** — they are false
positives or intentional design decisions. Two low-risk findings in a CI helper
script were hardened anyway.

How to apply: in the Snyk dashboard, mark each finding with the disposition and
reason below (False positive / Ignore → won't fix). Re-review on a finding only
if the surrounding code changes materially.

---

## Fixed in code (this change)

| Finding | Location | Fix |
|---|---|---|
| XXE (`xml.etree.ElementTree`) | `scripts/collect-flaky-tests.py` | switched to `defusedxml.ElementTree` (input is trusted Gradle JUnit XML, hardened regardless) |
| Path traversal (CLI arg → `open`) | `scripts/collect-flaky-tests.py` | `os.path.basename()` on the report name + reject `.`/`..` |

---

## False positive — mark "False positive" in the dashboard

| Finding | Location | Why it is a false positive |
|---|---|---|
| SQL Injection (CWE-89) | `KeyTrashController.kt:79`, `:105` | Query is built with the **JPA Criteria API** (`em.criteriaBuilder` → typed `CriteriaQuery`). Filter input becomes bound parameters, never concatenated SQL. |
| SQL Injection (CWE-89) | `SelectAllController.kt:74` | Same — JPA Criteria API via `TranslationsViewQueryBuilder`. |
| SQL Injection (CWE-89) | `TranslationsController.kt:280` | Same — JPA Criteria API. The only `createNativeQuery` on this path is a hardcoded `DROP TABLE temp_…`. |
| Regex Injection (CWE-400/730) | `GlossaryTermHighlightsController.kt:46` | Sink is `Regex.escape(search).toRegex()` in `GlossaryTermService.kt:282` — the user input is escaped before becoming a regex. |
| Regex Injection (CWE-400/730) | `PublicController.kt:93` | Sink is `ALIAS_REGEX.replaceFirst(email, …)` (email normalization). The **pattern is a constant**; user input is the input string, not the pattern. Not injection. |
| Hardcoded secret (CWE-547) | `useAuthService.tsx:27` | The flagged string is a **localStorage key name** (`'invitationCode'`/`'oauth2State'`/…), not a secret value. |
| Hardcoded credentials (CWE-798) | `UserAccountService.kt:659`, `:706` | The string is `"___implicit_user"`, an internal **username constant** used to look up the legacy implicit user — not a credential. |

---

## Won't fix — accepted by design (mark "Ignore → won't fix" with reason)

| Finding | Location | Reason |
|---|---|---|
| CSRF protection disabled (CWE-352) | `WebSecurityConfig.kt:83` | The API is **stateless Bearer-token auth** (`SessionCreationPolicy.STATELESS`, no cookie/session). CSRF does not apply when there is no ambient credential; enabling it would be incorrect. |
| SQL Injection (CWE-89) | `SqlController.kt:17`, `:25` | `@InternalController("internal/sql")` is a **dev-only SQL console** that runs request-body SQL by design. The `/internal/**` paths are not exposed in production. |
| Insecure hash MD5 (CWE-916) | `BaiduApiService.kt:67` | MD5 is required by **Baidu's translation API** for request signing (external protocol), not used as a security hash. |
| Hardcoded password (CWE-798/259) | `PostgresAutostartProperties.kt:32` | Default password for the optional **bundled dev/Docker Postgres** (container-local, overridable via config). |
| Hardcoded password (CWE-798/259) | `accountSecurity.cy.ts:29` | Cypress **e2e test fixture**, not production code. |
| Path traversal (CWE-23) | `report-flaky-tests.py:84` | Opens `GITHUB_OUTPUT`, the path the **GitHub Actions runner** provides for step outputs. Writing to it is the documented mechanism; the value is trusted runner infrastructure. |

---

## Notes

- Snyk Code repeatedly misses two safe patterns used heavily in this codebase:
  the **JPA Criteria API** (it sees data flow into `createQuery` and assumes
  string SQL) and **`Regex.escape(...)`** before `toRegex()`. Future findings of
  the same shape can be dispositioned the same way.
- Prefer per-issue dashboard dispositions over excluding whole paths from SAST,
  so genuinely new issues in these files are still surfaced.
