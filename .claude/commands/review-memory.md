---
description: Re-validate all long-term memory entries against current codebase
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Task
---

Use the memory-reviewer subagent to re-validate all long-term memory entries.

This will:
1. Check each entry's dependencies against the current codebase
2. Re-verify proofs for entries whose dependencies changed
3. Update validation timestamps for entries that are still valid
4. Correct entries that are "close but not quite right"

The subagent will spawn knowledge-critic agents in parallel to check each entry, then report results.
