# Long-Term Memory Entry Format

Reference for the `knowledge-critic` agent. This defines the structure and requirements for long-term memory entries.

---

## Entry Structure

Each entry in `long-term-memory/entries/` must follow this format:

```markdown
# [Descriptive Title]

**Added**: YYYY-MM-DD
**Tags**: #tag1, #tag2, #tag3

## Depends On

- `path/to/file.kt` - Brief description of what aspect matters
- `path/to/other/file.kt:45-67` - Specific line range if relevant
- https://docs.example.com/page - External documentation
- [[other-entry.md]] - Other memory entries this depends on

## Proof

[Explanation of WHY this knowledge is correct, referencing dependencies above]

## Validated

- **At**: YYYY-MM-DD HH:MM
- **Against**: abc1234 (git short revision)

## Context

[How/when this was discovered: "User correction", "Debugging session", etc.]

## Insight

[The actual knowledge - specific and actionable]

## Examples

[Code snippets or concrete examples, if applicable]

## Related

- [[other-entry.md]] - Related memories (NOT dependencies)
```

---

## Field Requirements

### Depends On

List ALL sources of truth this knowledge depends on. These are checked during re-validation.

| Dependency Type | Format | Example |
|-----------------|--------|---------|
| File in codebase | `path/to/file.kt` | `backend/data/.../TranslationService.kt` |
| Specific lines | `path/to/file.kt:45-67` | `Service.kt:52-58` |
| External docs | Full URL | `https://docs.spring.io/...` |
| Other entries | `[[entry-name.md]]` | `[[spring-proxy-gotcha.md]]` |

### Proof

Explains WHY the knowledge is correct, referencing specific dependencies.

**Good proof:**
> The `TranslationCommentService.create()` method at line 52 explicitly calls
> `entityManager.flush()` after persist. This ensures immediate write before
> response. Spring Data JPA docs confirm flush is not automatic outside @Transactional.

**Bad proof (don't do this):**
> I tested this and it worked.
> This is how Spring usually works.

### Validated

Updated each time the entry is verified:
- **At**: Timestamp of validation
- **Against**: Git short revision at validation time

### Tags

Use placeholder tags based on topic (e.g., `#spring`, `#hibernate`). The `knowledge-indexer` will refine these into the proper taxonomy.

---

## Deprecation Format

When an entry becomes invalid, delete it and remove it from all indexes.

### Deprecation Triggers

| Trigger | Action |
|---------|--------|
| Dependency file deleted | Deprecate - knowledge no longer applicable |
| Dependency significantly changed | Re-verify proof; deprecate if proof no longer holds |
| External URL 404 or changed | Try to find updated URL; deprecate if unverifiable |
| Proof logic no longer valid | Update proof if possible; deprecate if not |

---

## Validation Checklist

Before creating or approving an entry:

- [ ] Is this actually true? (Check the code)
- [ ] Is this generalizable? (Not a one-off situation)
- [ ] Would knowing this change future behavior?
- [ ] Can I identify specific dependencies?
- [ ] Can I write a verifiable proof?
- [ ] Is this already documented elsewhere?

If you cannot identify dependencies or write a proof, the observation may be:
- Too vague - make it more specific
- Opinion-based - not suitable for long-term memory
- Trivially rediscoverable - not worth storing
