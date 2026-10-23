# AI review

AI review lets a team define **rules** that say which translations an LLM may review and how strict
it must be. A translation that passes is marked **Reviewed**, so fewer strings wait for a human.

**Status:** in development (pitch #3955). This page describes the agreed design; parts of it are not
built yet.

This page explains how the feature works and records the decisions that were discussed at length.
If you change one of them, read its "Why" first: most of them protect against a bug that is easy to
bring back.

## 1. Overview

**Pieces:**

- **Rule** (`AiReviewRule`): a scope (tags, namespaces, languages, branch patterns), a threshold or
  Never, a mode (Dry run or Active), a review prompt, and language overrides.
- **Review row** (`TranslationAiReview`): one row per translation. It holds the result (status,
  score, issues) and is also the queue (`PENDING`).
- **Review prompt** (`Prompt` with `type = REVIEW`): asks the model for a list of issues with
  points. Tolgee computes `score = max(0, 100 − sum(points))`.
- **`AI_REVIEW` batch job**: calls the LLM and saves the result.

**Flow:**

```
text change ──► row PENDING ──► (QA on)  QA_CHECK job finished ──► AI_REVIEW job
                           └──► (QA off) AI_REVIEW job
manual run  ──► rows queued ──► visible AI_REVIEW job (can be cancelled)
```

The feature is a plan feature, `Feature.AI_REVIEW`. Without it, the UI shows nothing at all (no
tab, no upsell) and the API returns `FEATURE_NOT_ENABLED`.

## 2. How it works

1. A translation's text changes (human edit, MT, auto translation, import).
2. The listener finds the **winning rule** for the translation (see 3.1). No rule or Never: nothing
   happens.
3. The row becomes `PENDING`. When QA is on, the review waits until QA results for this text are
   fresh.
4. The job **claims** the row, calls the LLM, and saves the result:
   - Active rule, score at or above the threshold: state becomes **Reviewed**.
   - Active rule, below the threshold: state stays Translated, the issues are shown.
   - Dry run rule: only a score, the state never changes.
   - No usable answer: `FAILED`, nothing changes.
5. A human can **Confirm** the AI approval (it becomes a human review) or **Reject** it.

## 3. Decisions

### 3.1 Rule matching

- **Strictest rule wins.** Never beats any number; otherwise the highest threshold wins; on a tie a
  Dry run rule wins over an Active one, then the lowest rule id.
  *Why:* adding a tag can only make auto-review harder, so tagging a key `legal` is a reliable off
  switch.
- **Never is a blocker, not a mode.** It is checked across all matching rules, in every mode.
  *Why:* a Dry run rule with Never would otherwise contradict itself ("score it" vs "never look").
- **A threshold override is an exception to a rule-level Never** ("Legal: Never, but German at 98").
  *Why:* a separate rule can never beat a Never, so this is the only way to write an exception, and
  new languages stay protected.
- **Re-reviews of an existing AI approval use Active rules only.**
  *Why:* a Dry run rule must never change state. If it took over an approval, the row would stop
  saying `APPROVED` while the state stays Reviewed, and the system would read it as a human review.
- **Deleting a language never widens a rule.** If a rule's language list becomes empty, the rule is
  disabled. *Why:* an empty list means "all languages".
- **Tags and namespaces match exactly; only branches have `*` patterns.** `""` means "without tags"
  or "default namespace", the same as in the translations filters.

### 3.2 Scoring

- **Tolgee does not clamp the points** to the severity ranges, and the **severity is free text**.
  *Why:* an Advanced (custom) prompt may define its own ranges and severity names, which Tolgee
  cannot know. The default ranges (critical 75–100, major 25–50, minor 5–15) live in one constant.
- **Missing or broken points use the upper edge** of the default range, or 100 for an unknown
  severity. *Why:* a broken answer must never help a translation pass.
- **A score of 0 is never approved**, even with threshold 0.

### 3.3 Translation state and flags

- **An AI approval keeps the machine-translated mark** (`auto`, `mtProvider`). A human review clears
  it. *Why:* the pitch's source-change table has its own row "Approved by AI → Translate again". Auto
  translation only overwrites `auto` texts, so without the mark an AI-approved machine translation
  could never be translated again.
- **"Approved by AI" means `auto` + an AI approval.** A human-written text that the AI approved counts
  as "Edited by a human". *Why:* otherwise a translator's own text would be silently replaced by MT
  when the source changes.
- **"Keep, mark as outdated" on an AI approval clears `auto`.** *Why:* otherwise auto translation
  would still replace the text, and "Keep" would not keep it.
- **Any human un-review of an AI approval counts as a rejection.** That includes the popover's Reject,
  the normal state button and the batch "Mark as translated". The flags follow what that path does
  today (Reject and the state button clear `auto`, the batch path does not). *Why:* the "rejected"
  count is the success metric of the pilot, so every way of saying "the AI was wrong" must count.
- **Confirm turns the AI approval into a normal human review** (flags cleared, row `CONFIRMED`).
- **Protect reviewed:** AI approvals are protected like any review. But when an AI-approved text is
  translated again, it becomes Translated even in protected projects. "Reviewed by a human →
  Translate again" is locked while protection is on. *Why:* a string must never be Reviewed with text
  nobody (human or AI) has seen, and protection exists to keep human reviews.
- **Outdated translations are not reviewed.** *Why:* the text no longer matches its source, so the
  tokens would be wasted. It is reviewed again after someone edits it.

### 3.4 Results and their lifetime

- **One row per translation, bound to the text by a SHA-256 hash.** A row whose hash differs from the
  current text is treated as missing. Any text change deletes the row, even when the feature is off.
  *Why:* a row must never describe text it did not review.
- **A failed re-review keeps the previous result** and records `lastFailureReason`. *Why:* the pitch
  says "Failed … Nothing changes", and the old approval is still valid for the same text.
- **Demotion uses the stored score and never promotes.** A stricter rule can take an approval away
  without a new LLM call; a lower threshold never turns an old score into an approval. *Why:* the old
  score is good enough to remove trust, not to give it.
- **Deleting a rule keeps approval rows** (the rule reference becomes null) but deletes Dry run,
  failed and skipped rows. *Why:* if approval rows were deleted, their Reviewed translations would
  look like human reviews.

### 3.5 Concurrency

- **Order is text → QA → AI review, and runs never refuse**: a manual run only queues rows.
- **A guarded UPDATE "claims" a row** (`batch_job_id`) before the LLM call, in READ COMMITTED.
  *Why:* this makes duplicate jobs harmless across pods, so the LLM is never paid twice for the same
  text. Under REPEATABLE READ the same update would fail with a serialization error.
- **The translation row is locked (`SELECT … FOR UPDATE`) before the state is set.** *Why:* without
  the lock, Hibernate's full-column UPDATE can write an old text back over a concurrent auto
  translation (see PR #3979 for the same bug in QA).
- **`manual_run_batch_job_id`** records which manual run queued a row, separate from the job that
  claims it. *Why:* cancelling a run must undo only its own rows, while an automatic job may queue
  the same translation after an edit.
- **A prompt change switches its rules to Dry run**, and an LLM call that was already running with the
  old prompt is dropped (`Prompt.revision`). *Why:* an approval must never come from a prompt the rule
  no longer uses.

### 3.6 Activity log

- AI state changes are logged with `systemActor = AI_REVIEWER`. For a manual run the user who started
  it is kept as the author, and the UI shows "AI reviewer · started by <name>". *Why:* the user
  started the run, but the AI made each decision.
- Rule changes (`AI_REVIEW_RULE_*`) and source-change settings are logged, like webhooks and
  content-delivery configs. *Why:* rules decide what gets Reviewed without a human, so an audit trail
  matters.

### 3.7 Caching

- Rules are cached per project (`Caches.AI_REVIEW_RULES`). **Every** write to rules, their overrides
  or their languages must go through `AiReviewRuleService`, which evicts the cache.
