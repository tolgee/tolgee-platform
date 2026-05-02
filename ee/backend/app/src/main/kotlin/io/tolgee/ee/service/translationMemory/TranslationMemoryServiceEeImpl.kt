package io.tolgee.ee.service.translationMemory

import io.tolgee.service.translation.AbstractTranslationMemoryService
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

/**
 * EE-only suggestion service. On top of the OSS virtual-rows path, this implementation also unions
 * stored entries from `translation_memory_entry` — synced + manual rows on shared TMs, plus manual
 * rows on project TMs — and applies the per-TM/per-assignment penalty to non-PROJECT matches.
 *
 * Annotated `@Primary` so it overrides
 * [io.tolgee.service.translation.TranslationMemoryServiceOssImpl] whenever the EE module is on
 * the classpath.
 */
@Service
@Primary
class TranslationMemoryServiceEeImpl(
  entityManager: EntityManager,
  translationMemoryManagementService: TranslationMemoryManagementService,
) : AbstractTranslationMemoryService(entityManager, translationMemoryManagementService) {
  /**
   * Raw match rows, unioned across the two entry sources:
   *
   * 1. **Stored entries** — shared TMs (synced + manual) and manual rows on project TMs.
   *    Key name comes from any one contributing translation via `translation_memory_entry_source`
   *    for synced rows, or is NULL for manual rows.
   * 2. **Virtual entries** — computed on the fly for each project TM in `:tmIds` from the
   *    current project's translations. Filtered by default branch, non-empty text, and
   *    `writeOnlyReviewed` state if set on the TM.
   *
   * The `:keyId` exclusion runs independently in each branch using the schema available there
   * (join-table lookup for stored synced entries, direct `k.id` comparison for virtual).
   */
  override val baseSelect: String = """
    select re.target_text as targetTranslationText,
           re.source_text as baseTranslationText,
           re.key_name as keyName,
           re.key_namespace as keyNamespace,
           re.key_id as keyId,
           similarity(re.source_text, :baseTranslationText) as rawSimilarity,
           case
             when tm.type = 'PROJECT' then similarity(re.source_text, :baseTranslationText)
             else greatest(
               similarity(re.source_text, :baseTranslationText)
                 - (coalesce(tmp.penalty, tm.default_penalty) / 100.0),
               0
             )
           end as similarity,
           tm.name as translationMemoryName,
           tmp.priority as assignmentPriority,
           re.updated_at as updatedAt
    from (
      -- Stored entries: shared TMs (both synced and manual) + manual rows in project TMs.
      -- Key name is aggregated from any one contributing translation's key for synced entries;
      -- manual entries (no source rows) leave it null.
      select tme.target_text,
             tme.source_text,
             tme.target_language_tag,
             tme.translation_memory_id as tm_id,
             kn.key_name,
             kn.key_namespace,
             coalesce(kn.key_id, 0) as key_id,
             kn.any_key_is_plural as any_key_is_plural,
             kn.includes_current_key as includes_current_key,
             tme.updated_at as updated_at
      from translation_memory_entry tme
      join translation_memory tm_src on tm_src.id = tme.translation_memory_id
      left join lateral (
        select min(k.name) as key_name,
               min(ns.name) as key_namespace,
               min(k.id) as key_id,
               bool_or(k.is_plural) as any_key_is_plural,
               bool_or(cast(:keyId as bigint) is not null and k.id = :keyId) as includes_current_key
        from translation_memory_entry_source s
        join translation t on t.id = s.translation_id
        join key k on k.id = t.key_id
        left join namespace ns on ns.id = k.namespace_id
        where s.entry_id = tme.id
      ) kn on true
      where tme.translation_memory_id in :tmIds
        and tme.target_language_tag = :targetLanguageTag
        and tme.source_text % :baseTranslationText
        and (tm_src.type <> 'PROJECT' or tme.is_manual = true)

      union all

      -- Virtual entries: computed from the project's translations for every PROJECT-type TM
      -- in :tmIds. We still emit the tm_id so the outer select can apply tm-level config
      -- (penalty, priority, name) from the matching translation_memory row. updated_at comes
      -- from the contributing target translation — that's what users perceive as the "match
      -- age" for a virtual row, since the row materialises from the translation.
      --
      -- Driven by a trigram-filtered subquery on `translation` so the planner is forced to
      -- use `translation_lang_text_gist_trgm` (language_id, text) before any other join.
      -- Joining `key` and target translations off that small candidate set keeps the cost
      -- proportional to the number of matched rows, not to the project's total key count.
      select
        target_t.text as target_text,
        base_match.text as source_text,
        target_lang.tag as target_language_tag,
        tm_virt.id as tm_id,
        k.name as key_name,
        ns.name as key_namespace,
        k.id as key_id,
        k.is_plural as any_key_is_plural,
        (cast(:keyId as bigint) is not null and k.id = :keyId) as includes_current_key,
        target_t.updated_at as updated_at
      from (
        select base_t.key_id, base_t.text, base_t.language_id
        from translation base_t
        where base_t.language_id =
              (select p.base_language_id from project p where p.id = :projectId)
          and base_t.text <> ''
          and base_t.text is not null
          and base_t.text % :baseTranslationText
      ) base_match
      join key k on k.id = base_match.key_id and k.project_id = :projectId
      left join namespace ns on ns.id = k.namespace_id
      left join branch b on b.id = k.branch_id
      join translation target_t
        on target_t.key_id = k.id and target_t.language_id <> base_match.language_id
      join language target_lang on target_lang.id = target_t.language_id
      join translation_memory tm_virt
        on tm_virt.id in :tmIds and tm_virt.type = 'PROJECT'
      where target_lang.tag = :targetLanguageTag
        and target_t.text is not null and target_t.text <> ''
        and (b.id is null or b.is_default = true)
        and (not tm_virt.write_only_reviewed or target_t.state = 2)
    ) re
    join translation_memory tm on tm.id = re.tm_id
    left join translation_memory_project tmp
      on tmp.translation_memory_id = re.tm_id
      and tmp.project_id = :projectId
    where (cast(:keyId as bigint) is null or not coalesce(re.includes_current_key, false))
      and (re.any_key_is_plural is null or re.any_key_is_plural = :isPlural)
  """
}
