package io.tolgee.service.translation

import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

/**
 * OSS suggestion service. Free plans only ever query the project's own (PROJECT-type) TM, whose
 * content is computed virtually from the project's translations on demand — no `translation_memory_entry`
 * rows are involved in this path.
 *
 * The EE module overrides this bean with `@Primary` to add the stored-entries half of the union
 * (synced + manual rows on shared TMs, manual rows on project TMs).
 */
@Service
class TranslationMemoryServiceOssImpl(
  entityManager: EntityManager,
  translationMemoryManagementService: TranslationMemoryManagementService,
) : AbstractTranslationMemoryService(entityManager, translationMemoryManagementService) {
  /**
   * Virtual rows only — computed from the project's translations for every PROJECT-type TM in
   * `:tmIds`. Driven by a trigram-filtered subquery on `translation` so the planner is forced to
   * use `translation_lang_text_gist_trgm` (language_id, text) before any other join. Joining
   * `key` and target translations off that small candidate set keeps the cost proportional to the
   * number of matched rows, not to the project's total key count.
   *
   * Project TMs ignore the per-assignment penalty (they always represent the user's own work),
   * so similarity collapses to raw similarity; the EE override re-introduces the penalty case for
   * shared TMs.
   */
  override val baseSelect: String = """
    select re.target_text as targetTranslationText,
           re.source_text as baseTranslationText,
           re.key_name as keyName,
           re.key_namespace as keyNamespace,
           re.key_id as keyId,
           similarity(re.source_text, :baseTranslationText) as rawSimilarity,
           similarity(re.source_text, :baseTranslationText) as similarity,
           tm.name as translationMemoryName,
           tmp.priority as assignmentPriority,
           re.updated_at as updatedAt
    from (
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
