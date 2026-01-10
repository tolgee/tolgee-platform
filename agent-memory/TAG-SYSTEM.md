# Tag System Reference

Reference for the `knowledge-indexer` agent. This defines how tags and indexes work.

---

## Concepts

### Entry-Level Tags

Tags applied directly to memory entries. These are specific and describe the content.

Examples: `#spring-transactions`, `#hibernate-gotcha`, `#kotlin-null-safety`

### Meta-Tags

Tags that group other tags. Meta-tags can be nested to any depth.

Examples: `#spring` (groups `#spring-transactions`, `#spring-security`), `#gotcha` (groups `#hibernate-gotcha`, `#kotlin-gotcha`)

### Relationship

```
(root)
├── #backend          ← meta-tag
│   ├── #spring       ← meta-tag (child of #backend)
│   │   ├── #spring-transactions  ← entry-level tag
│   │   └── #spring-security      ← entry-level tag
│   └── #hibernate    ← meta-tag
│       └── #hibernate-gotcha     ← entry-level tag
└── #gotcha           ← meta-tag
    ├── #hibernate-gotcha         ← entry-level tag (can have multiple parents)
    └── #kotlin-gotcha            ← entry-level tag
```

A tag can appear under multiple parents (this is a DAG, not a strict tree).

---

## File Structure

### TAGS.md (Taxonomy Data)

Location: `long-term-memory/TAGS.md`

Contains three sections:
1. **Entry-Level Tags** - Table of tags with descriptions and parent meta-tags
2. **Meta-Tags** - Table of meta-tags with their children
3. **Meta-Tag Graph** - ASCII tree showing the hierarchy

### INDEX.md (Navigation)

Location: `long-term-memory/INDEX.md`

Top-level navigation showing:
- Topics table (top-level meta-tags only)
- All Entries table (flat lookup)
- Quick stats

### Tag Index Files

Location: `long-term-memory/indexes/<tag>.index.md`

Each tag with 3+ entries gets an index file:

```markdown
# #<tag> Index

## Sub-Topics
| Tag | Description | Entries |
|-----|-------------|---------|
| #child-tag | Description | 5 |

## Entries
| Entry | Tags | Brief |
|-------|------|-------|
| [[entry-name.md]] | #tag1, #tag2 | One-line summary |

---
_Last updated: YYYY-MM-DD_
```

---

## Tag Naming Conventions

| Rule | Good | Bad |
|------|------|-----|
| Lowercase | `#spring` | `#Spring` |
| Hyphens for multi-word | `#spring-transactions` | `#springTransactions` |
| Specific but not verbose | `#hibernate-gotcha` | `#hibernate-lazy-loading-outside-session-gotcha` |
| Avoid redundancy | `#transactions` + `#spring` meta-tag | `#spring-transactions` if ONLY for Spring |

---

## When to Create New Tags

### Create a new entry-level tag when:
- Existing tags don't capture the concept well
- The concept is likely to recur (not a one-off)
- The concept is distinct enough to warrant its own tag

### Don't create when:
- An existing tag is "close enough"
- The concept is too narrow (would only ever have 1-2 entries)
- The concept overlaps significantly with existing tags

### Create a new meta-tag when:
- A clear grouping emerges among entry-level tags
- You have 3+ tags that belong together
- The grouping would help with navigation/discovery

---

## Rebalancing

Consider rebalancing when:
- A meta-tag has >15 direct children - split into sub-meta-tags
- Tags always appear together - consider merging or creating intermediate grouping
- A concept has been mis-categorized - move tags between meta-tags

---

## Indexing Process

When indexing a new entry:

1. **Read TAGS.md** to understand existing taxonomy
2. **Assign entry-level tags** (prefer existing, create new if needed)
3. **If new tags created**: Apply meta-tags, update TAGS.md
4. **Update entry file** with final tags
5. **Update index files** for each assigned tag
6. **Update INDEX.md** entry counts and All Entries table
