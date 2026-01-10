# Long-Term Memory Index

This is the navigation entry point for all curated knowledge.

## How to Use

1. Browse the **Topics** table below for high-level categories
2. Click through to topic index files for more specific tags
3. Find entries by tag in the index files
4. Or use the **All Entries** table for direct lookup

---

## Topics

Top-level categories. Each links to a more detailed index.

| Topic | Description | Entries | Index |
|-------|-------------|---------|-------|
| _(populated as entries accumulate)_ | | | |

---

## All Entries

Flat lookup table for quick search across all memories.

| Entry | Tags | Brief |
|-------|------|-------|
| _(populated during consolidation)_ | | |

---

## Quick Stats

- **Total entries**: 0
- **Total tags**: 0 (0 entry-level, 0 meta-tags)
- **Last consolidation**: Never
- **Last review**: Never

---

## Related Files

| File | Purpose |
|------|---------|
| `TAGS.md` | Tag taxonomy and hierarchy (source of truth for tags) |
| `indexes/*.index.md` | Per-tag index files |
| `entries/*.md` | Individual memory entries |

---

## Maintenance

### Adding new knowledge
Run `/consolidate-memory` to process observations from short-term memory.

### Reviewing existing knowledge
Run `/review-memory` to re-validate all entries against current codebase.

### Understanding the system
- New observations are captured automatically (Stop hook)
- Observations are validated by `knowledge-critic` agents
- Valid entries are indexed by `knowledge-indexer` agents
- The tag taxonomy in `TAGS.md` is maintained automatically

---

_Last updated: (auto-update during consolidation/review)_
