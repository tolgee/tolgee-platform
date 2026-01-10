---
description: Consolidate short-term observations into long-term memory
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

Use the memory-consolidator subagent to transform observations from agent-memory/short-term-memory/ into long-term entries.

The subagent will:
- Evaluate each observation (novel? verified? generalizable? actionable?)
- Create entry files for qualifying observations
- Assign tags
- Rebuild tag indexes
- Update the main INDEX.md
- Report what was done

Let the subagent handle the full process and report results.
