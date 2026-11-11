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

- **Rule** (`AiReviewRule`): a required name, a scope (tags, namespaces, languages, branch
  patterns), a threshold or Never, a mode (Score only or Active), and a review prompt.
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
tab, no upsell) and the API returns `FEATURE_NOT_ENABLED`. One exception: the "When the source text
changes" settings in the Advanced project settings work for every project. Only their "Approved by
AI" row needs the feature.

On top of the plan feature, each project has its own switch (`Project.useAiReview`, off by
default). While it is off, the review endpoints (detail, retry, run preview, run, playground
review) return `FEATURE_NOT_ENABLED_FOR_PROJECT`, but rules and review prompts stay manageable.
Turning it on creates one default rule ("Default rule": Score only, threshold 85, skip open QA
issues, all languages) when the project has no rules yet — never a second one. Turning it off
deletes the `PENDING` queue rows and clears re-review flags; stored results and overridden markers
stay.

## 2. How it works

1. A translation's text changes (human edit, MT, auto translation, import).
2. The listener finds the **winning rule** for the translation (see 3.1). No rule or Never: nothing
   happens.
3. The row becomes `PENDING`. When QA is on, the review waits until QA results for this text are
   fresh.
4. The job **claims** the row, calls the LLM, and saves the result:
   - Active rule, score at or above the threshold: state becomes **Reviewed**.
   - Active rule, below the threshold: state stays Translated, the issues are shown.
   - Score only rule: only a score, the state never changes.
   - No usable answer: `FAILED`, nothing changes.
5. A human decision wins over the AI. When a human reviews or un-reviews a translation that has an
   AI result, the row becomes an `OVERRIDDEN` marker that names the user (see 3.3).

## 3. Decisions

### 3.1 Rule matching

- **Strictest rule wins.** Never beats any number; otherwise the highest threshold wins; on a tie a
  Score only rule wins over an Active one, then the lowest rule id.
  *Why:* adding a tag can only make auto-review harder, so tagging a key `legal` is a reliable off
  switch.
- **Never is a blocker, not a mode.** It is checked across all matching rules, in every mode.
  *Why:* a Score only rule with Never would otherwise contradict itself ("score it" vs "never
  look").
- **Re-reviews of an AI approval use Active rules only.** This includes the re-review of an
  AI-managed review after MT replaced its text under Protect reviewed, where the state stays Reviewed
  whatever the result is. *Why:*
  - The re-review decides whether the text may be "Approved by AI" again. Only an Active rule can
    approve. A Score only result would replace the approval.
  - A Reviewed translation with a Score only row is not an AI-managed review, so the system would
    read it as a human review. The AI would stop checking it, and the next MT text would stay
    Reviewed unseen. Counting Score only rows as AI-managed is not safe either: a bulk-reviewed
    text can have a leftover Score only row.
  - "Strictest wins" includes Score only rules. A stricter rule tested in Score only would take
    over the approvals after the next source change. Score only must never change anything live.
  The cost: these translations get no Score only scores. If no Active rule wins for such a
  translation, or it can no longer be reviewed (for example it is outdated), its row stays: an
  approval stays `APPROVED`, any other row becomes `FAILED`. It leaves the queue, and the
  translation stays an AI-managed review. This is the same when the job claims the row and when
  the text changes, so the result does not depend on timing.
- **Demotion compares the stored score with the strictest Active rule.** A matching Score only
  rule never blocks a demotion, and an approval whose matching rules are all Score only stays.
  *Why:* Score only must never change anything live.
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

### 3.2.1 Review prompt

- The source text and the translation go into the prompt as **JSON strings** (in quotes, already
  escaped). *Why:* a text cannot close the string and add its own instructions. The prompt also
  says that a text that gives instructions is a critical issue.
- The **response schema** travels in `LlmParams.responseSchema` on every review call. There is no
  central switch: each provider service decides. The OpenAI and Anthropic services send it when the
  provider config has `format: json_schema`; Google AI and Ollama ignore the field and rely on the
  text instructions in the prompt (format block, example, output rules); the Tolgee provider
  forwards the whole `LlmParams` to Tolgee cloud, which decides the same way.
- The default review prompt does not use the translator fragments `intro`, `styleInfo`, `icuInfo`,
  `translationInfo` and `translateJson`. *Why:* they ask the model to translate, and `icuInfo` asks
  it to return plural forms.

### 3.3 Translation state and flags

- **An AI approval keeps the machine-translated mark** (`auto`, `mtProvider`). A human review clears
  it. *Why:* the pitch's source-change table has its own row "Approved by AI → Translate again". Auto
  translation only overwrites `auto` texts, so without the mark an AI-approved machine translation
  could never be translated again.
- **"Approved by AI" means `auto` + an AI approval.** A human-written text that the AI approved counts
  as "Edited by a human". *Why:* otherwise a translator's own text would be silently replaced by MT
  when the source changes.
- **"Keep, mark as outdated" on an AI approval clears `auto`** (only without Protect reviewed; with
  it, Keep is not offered for AI approvals). *Why:* otherwise auto translation would still replace
  the text, and "Keep" would not keep it. Open point: after this the text is still machine
  translated, so the missing mark is not true. A better way is not decided yet.
- **Any human un-review of an AI approval marks the row `OVERRIDDEN`.** That includes the normal
  state button and the batch "Mark as translated". The flags follow what that path does today (the
  state button clears `auto`, the batch path does not). The marker keeps who resolved it and when,
  and the badge shows a quiet chip on hover. *Why:* the AI must never undo a human decision, and
  the marker tells the next reader that the AI result was overruled, by whom.
  Retry is hidden for an overridden row, and the retry endpoint refuses it. Only a manual run with
  the re-review option reviews it again.
  A human text edit that un-reviews the text drops the old row (the hash no longer matches). A
  machine overwrite also drops it: when the same change writes new text with `auto` (bulk "Machine
  translate", "Pre-translate by TM"), the approval is deleted. *Why:* MT that replaces the text
  does not say the AI was wrong.
  While the feature or the project switch is off, a human un-review deletes the approval instead.
  *Why:* a kept marker on a project that opted out blocks later runs for no reason.
- **A human review over a bad AI result also marks the row `OVERRIDDEN`.** When a human sets
  Reviewed on a text with a current `BELOW_THRESHOLD`, `SCORE_ONLY_PASS` or `SCORE_ONLY_FAIL` row
  (and AI review is on for the project), the row converts instead of being deleted. *Why:* the
  human overruled the AI here too, and the marker keeps that visible.
- **The "auto-reviewed" count is a counter on the rule** (`auto_reviewed_count`). It grows when the
  AI approves a text. A later text change deletes the row but does not lower the count.
- **Only the rule list returns the counts.** Create, update and enable responses have
  `counts = null`, and the webapp reads the counts from the reloaded list. *Why:* the backlog scans
  the whole project, and the list is reloaded after every change anyway.
- **Protect reviewed keeps the Reviewed state, also for AI approvals.** Under protection the AI never
  sets a Reviewed translation to Translated: not when the source text changes, not after a failed
  re-review, not on demotion. *Why:* the team rule is that a protected Reviewed translation stays
  Reviewed no matter what. Human actions (the state button) still work as today.
- **Under protection an AI-approved machine translation is translated again like in main.** It keeps
  `auto`, so MT writes new text when the source changes, and the state stays Reviewed. The setting
  "Approved by AI" is locked to "Translate again". The old row is replaced by a `PENDING` row, and
  the AI reviews the new text with Active rules only. Passed: "Approved by AI" again. Failed: stays
  Reviewed, the row is `BELOW_THRESHOLD`, and the badge shows the score and the issues. *Why:* main
  translates Reviewed `auto` texts again, and clients rely on that. Clearing `auto` or blocking MT
  would change main. The re-review makes sure "Approved by AI" is never shown for text the AI has
  not seen.
- **"AI-managed review"** (`AiReviewManaged`): a Reviewed translation with an AI approval or a
  below-threshold result for the current text, or with `auto` and an AI row that is not
  `OVERRIDDEN` or Score only. It is not human-reviewed, so the AI may review it again.
  *Why:* after MT replaced an approval, or after a demotion or a failed re-review under protection,
  the row is no longer an approval, but the state is still the AI's. This also holds for
  human-written text (no `auto`) that the AI approved. When a human sets Reviewed, the activity
  listener converts or deletes every AI row that would keep the translation AI-managed, so a human
  review never matches. The bulk "Change state" keeps `auto` (as in main), so the listener is
  needed also for `auto` texts. *Why:* otherwise a later re-review could set a human review back to
  Translated. For a batch job, the listener handles human state changes in each chunk's own
  transaction, not when the job ends. *Why:* a job that claims the row after a chunk commits would
  see an AI-managed review. The listener's delete or `OVERRIDDEN` commits together with the human
  change, so a job that claimed the row before finds no row or a human decision and drops its
  result. In the same way, a re-review of an AI approval drops its result when a human un-reviews
  the approval during the LLM call.
  *Why:* the AI must never undo a human decision.
- **The server decides if a result still applies to the state** (`AiReviewResultApplies`): an
  approval only while Reviewed, below threshold while Translated or Reviewed, failed only while
  Translated; other results always. The translations view sends a summary only when it
  applies, the outcome filter uses the same rule, and websocket updates send a removal otherwise.
  The webapp shows what the server sends. *Why:* two copies of this rule drifted apart, and a
  Translated string must never show "Reviewed by AI".
- **"Reviewed by a human → Translate again" is locked while protection is on.** *Why:* a human
  "Translate again" sets Translated, which protection does not allow.
- **"Translate again" is used only when auto translation really runs for this change**: the same
  activity, import and per-language config rules as `AutoTranslationService.startsForActivity`.
  Otherwise the setting falls back to Keep. *Why:* a text marked `auto` that no auto translation
  replaces looks machine translated. When auto translation runs but writes nothing (no TM match, no
  credits), the text still keeps `auto`.
- **The human source change settings work without `AI_REVIEW`.** Only "Approved by AI" needs the
  feature: without it, that row is hidden, its stored value is kept, and it behaves like the default.
  *Why:* what happens to human work on a source change is a project setting, not part of AI review.
- **Outdated translations and translations of trashed keys or branches are not reviewed.** *Why:*
  an outdated text no longer matches its source, so the tokens would be wasted. It is reviewed
  again after someone edits it.

### 3.4 Results and their lifetime

- **One row per translation, bound to the text by a SHA-256 hash.** A row whose hash differs from the
  current text is treated as missing. Any text change deletes the row, even when the feature is off.
  *Why:* a row must never describe text it did not review.
- **Bulk deletes keep AI-managed reviews AI-managed** (`AiReviewManaged`, one rule with a Kotlin and
  a JPQL form). A text change replaces such a row with a `FAILED` row for the new text, also when
  the feature is off. A source change and a rule delete keep the row. The only exception is the
  base language change (below). *Why:* without the row, the Reviewed state looks like a human
  review, and the AI never sees the next MT text.
- **A `FAILED` row without a failure reason is only a marker** for an AI-managed review. It is not
  counted as failed in the rule counts, and "Retry failed" skips it. A human un-review deletes it,
  also while the feature is off, so the Translated text goes back to the backlog. *Why:* no LLM
  call failed.
- **A failed re-review keeps the previous result** and records `lastFailureReason`. *Why:* the pitch
  says "Failed … Nothing changes", and the old approval is still valid for the same text.
- **The rule, mode, threshold and prompt on a row describe its result**, not the last attempt. The
  job writes them when it stores a new result. The claim only takes the row (`batch_job_id`). A
  failed re-review keeps the previous result and its rule. A demotion to below threshold writes the
  demoting rule; a demotion that keeps the approval keeps its rule. *Why:* the counters and the
  popover must name the rule and prompt that produced the result.
- **Demotion uses the stored score and never promotes.** A stricter rule can take an approval away
  without a new LLM call; a lower threshold never turns an old score into an approval. *Why:* the old
  score is good enough to remove trust, not to give it.
- **Deleting a rule keeps approval rows** (the rule reference becomes null) but deletes Score only,
  failed and skipped rows, except the rows of AI-managed reviews. *Why:* if approval rows were deleted, their Reviewed translations would
  look like human reviews. Later demotion runs keep these approvals too (also for a disabled rule):
  only a matching rule that is stricter than the stored score, or a Never, removes them. A rule edit
  that changes only the name starts no demotion.
- **Scope drift starts a demotion.** Moving a key to another namespace, changing its tags, or
  renaming a branch re-checks approvals, so a Never or stricter rule that now matches them takes
  effect. A namespace rename instead rewrites the rules' scope lists, and deleting a rule's only
  language disables the rule. *Why:* scoping must stay a reliable off switch when the things it
  matches on change under it.
- **A base language change deletes all rows except human decisions** (`OVERRIDDEN`).
  *Why:* every stored score was measured against the old base language. The cost: a Reviewed
  translation that the AI approved keeps its state and `auto` but loses its row, so it counts as a
  human review and "Approved by AI" is no longer shown.

### 3.5 Concurrency

- **Order is text → QA → AI review, and a run never waits for other work**: a manual run only queues
  rows. A run with nothing to review starts no job and returns `AI_REVIEW_NOTHING_TO_REVIEW`. *Why:* a
  job with no chunks never finishes and blocks self-heal for the project.
- **Each trigger starts its own hidden `AI_REVIEW` job with a fixed target list** (no debounce), and
  self-heal (after each QA or AI review job, and when the feature comes back) picks up rows that no
  job holds. *Why:* the batch framework needs the target list when the job is created, and the
  debounce would return the running job and drop the new targets. This shape is temporary. The
  planned follow-up is one queue-reading job per project that claims ready rows itself
  (`for update skip locked`). Then triggers only write `PENDING` rows, and the follow-up deletes:
  `selfHeal` and its project-wide scans, `handlingStuckItems` on `AiReviewRequest`, the
  `excludedJobId` parameter on `BatchJobService.hasActiveJobForProject`, the revision ordering and
  type coalescing added to `BatchJobActivityFinalizer`, and the claim re-pointing in
  `AiReviewBatchServiceEeImpl.refreshForCurrentText`. The queue columns on the result row
  (`PENDING`, `batch_job_id`, `manual_run_batch_job_id`, `re_review_requested`) then move to a
  separate queue table.
- **One chunk execution can own several activity revisions.** An AI review chunk claims and saves
  each translation in its own short transaction (`AiReviewBatchServiceEeImpl.reviewTranslation`),
  and each of those transactions gets its own `ActivityHolder`, so it writes its own activity
  revision. *Why:* the slow LLM call must run outside any open database transaction, and a saved
  result must survive when a later item in the chunk fails. This is why
  `BatchJobActivityFinalizer` needs the revision ordering and type coalescing listed above when it
  merges the job's revisions into one, and why `ActivityRevision.batchJobChunkExecution` is not
  unique per execution.
- **Known costs of the interim shape.** Self-heal runs after every finished QA or AI review job, so
  one edit in a project with QA on runs two project-wide queue scans. Two AI review jobs that
  finalize at the same time can each see the other as active, so neither self-heals; the rows then
  wait for the next job in the project. A bulk text change (import, machine translate all) loads
  the affected translations as entities inside the finalize transaction. All of this goes away with
  the queue-reading job above, so it is accepted, not fixed, in this shape.
- **Queued rows are inserted with `on conflict (translation_id) do nothing`.** *Why:* a user edit and a
  run start can queue the same translation at the same time, and a unique violation would abort the
  user's own transaction.
- **A guarded UPDATE "claims" a row** (`batch_job_id`) before the LLM call, in READ COMMITTED.
  *Why:* this makes duplicate jobs harmless across pods, so the LLM is never paid twice for the same
  text. Under REPEATABLE READ the same update would fail with a serialization error.
- **The translation row is locked (`SELECT … FOR UPDATE`) before the state is set.** *Why:* without
  the lock, Hibernate's full-column UPDATE can write an old text back over a concurrent auto
  translation (see PR #3979 for the same bug in QA).
- **`manual_run_batch_job_id`** records which manual run queued a row, separate from the job that
  claims it. *Why:* cancelling a run must undo only its own rows, while an automatic job may queue
  the same translation after an edit.
- **A prompt change switches its rules to Score only**, and an LLM call that was already running
  with the old prompt is dropped (`Prompt.revision`). *Why:* an approval must never come from a
  prompt the rule no longer uses.
- **Known limit: a change of the LLM model or of the built-in default review prompt does not switch
  rules to Score only.** A rule without a prompt uses the built-in prompt, which can change in a
  Tolgee release. A prompt that uses the organization's default provider gets a new model when an
  admin changes the provider. Scores depend on the model and the prompt, so after such a change a
  team should test its thresholds again in Score only. Tolgee does not detect this yet.

### 3.6 Activity log

- AI state changes are logged with `systemActor = AI_REVIEWER`. For a manual run the user who started
  it is kept as the author, and the UI shows "AI reviewer · started by <name>". *Why:* the user
  started the run, but the AI made each decision.
- Rule changes (`AI_REVIEW_RULE_*`) and source-change settings are logged, like webhooks and
  content-delivery configs. *Why:* rules decide what gets Reviewed without a human, so an audit trail
  matters.

### 3.7 Caching

- Rules are cached per project (`Caches.AI_REVIEW_RULES`). **Every** write to rules or their
  languages must go through `AiReviewRuleService`, which evicts the cache.
