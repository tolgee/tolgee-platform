# Tolgee Platform - Agent Instructions

## Memory System

This project uses a hierarchical memory system with long-term memories that can be loaded on demand.

### Loading Memories

Use the **memory-loader** agent to retrieve relevant long-term memories. Call it:
- After gathering enough context to understand the task (not at conversation start)
- When context changes significantly (e.g., discovering the task involves different areas than initially thought)
- When encountering unexpected behavior that might be documented in memory

The agent searches long-term memory for relevant entries and tracks what's been loaded to avoid duplicates across invocations.

### Working Memory (Scratch-Pad)

Use `agent-memory/working-memory/` for task-specific artifacts:
- Plans and intermediate notes
- TODO's/Checklists to track progress
- Offloading task-specific state that isn't used all the time
- Task state that should persist through context compaction
- **Not** for storing knowledge - that goes in short-term memory

**Segmentation by agent**: To avoid collisions, each agent uses its own subdirectory:
- Toplevel agent: `agent-memory/working-memory/__main/`
- Subagents: `agent-memory/working-memory/<agent-name>/` (e.g., `memory-loader/`)

### Capturing Observations

The **Stop hook** prompts you to capture observations at turn end. Assess:

| Question | If Yes → Action |
|----------|-----------------|
| Did user correct me? | **HIGH VALUE** - Capture mistake and correction |
| Did user express a preference? | Capture the preference |
| Did I backtrack or change approach? | **HIGH VALUE** - Capture why the first approach failed |
| Discovered something non-obvious? | Capture the discovery |
| Wasted time on something avoidable? | **HIGH VALUE** - Capture prevention strategy |

Append to `agent-memory/short-term-memory/observations.md`:
```markdown
## [YYYY-MM-DD HH:MM] Category: brief-title

What happened and what to remember. Be specific and include relevant context, but don't be verbose just for the sake of verbosity - if there's a simple way to say things, do so.
```

### Memory Commands

- **`/consolidate-memory`** - Suggest when short-term memory accumulates or a task finishes. Promotes validated observations to long-term memory.
- **`/review-memory`** - Suggest after significant codebase changes, or after enough time has passed since last review. Determine that duration using git, by comparing commit where agent-memory/ was last modified vs. current commit. Re-validates existing long-term memories.

---

## What to Remember

The most valuable observations:
1. **Prevent repeated mistakes** - Corrections, gotchas, edge cases
2. **Save time** - Shortcuts, patterns, "where to find X"
3. **Encode non-obvious knowledge** - Things not apparent from code alone
4. **Capture architectural decisions** - Why things are the way they are

Skip: Trivial facts easily re-discovered by reading code
