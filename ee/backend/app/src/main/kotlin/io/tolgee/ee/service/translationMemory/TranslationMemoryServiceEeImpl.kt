package io.tolgee.ee.service.translationMemory

import io.tolgee.service.translation.AbstractTranslationMemoryService
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

/**
 * EE suggestion service. Unions stored entries with virtual rows computed from every project
 * assigned to a TM with `writeAccess=true`. Penalty applies on top of similarity for any TM
 * other than the calling project's own (i.e., the assignment that pairs `:projectId` and
 * the matched TM). `@Primary` so the EE bean overrides the OSS one when on the classpath.
 */
@Service
@Primary
class TranslationMemoryServiceEeImpl(
  entityManager: EntityManager,
  translationMemoryManagementService: TranslationMemoryManagementService,
) : AbstractTranslationMemoryService(entityManager, translationMemoryManagementService) {
  override val baseSelect: String = """
    select re.target_text as targetTranslationText,
           re.source_text as baseTranslationText,
           re.key_name as keyName,
           re.key_namespace as keyNamespace,
           re.key_id as keyId,
           similarity(re.source_text, :baseTranslationText) as rawSimilarity,
           greatest(
             similarity(re.source_text, :baseTranslationText)
               - (coalesce(tmp.penalty, tm.default_penalty) / 100.0),
             0
           ) as similarity,
           tm.name as translationMemoryName,
           tmp.priority as assignmentPriority,
           re.updated_at as updatedAt
    from (
      -- Stored entries: user-created via the add-entry dialog or TMX import. No contributing
      -- key — manual entries are not linked to any project translation.
      select tme.target_text,
             tme.source_text,
             tme.target_language_tag,
             tme.translation_memory_id as tm_id,
             null::text as key_name,
             null::text as key_namespace,
             0::bigint as key_id,
             null::boolean as any_key_is_plural,
             false as includes_current_key,
             tme.updated_at as updated_at
      from translation_memory_entry tme
      where tme.translation_memory_id in :tmIds
        and tme.target_language_tag = :targetLanguageTag
        and tme.source_text % :baseTranslationText

      union all

      -- Virtual entries: for every TM in :tmIds, compute rows from each write-access-assigned
      -- project's translations. The trigram-filtered subquery on `translation` is the leading
      -- scan so the GIST index `translation_lang_text_gist_trgm` drives the plan; subsequent
      -- joins fan out to find which (project, TM) pairs each candidate contributes to.
      -- updated_at comes from the contributing target translation — that's the "match age"
      -- the user perceives.
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
        where base_t.text <> ''
          and base_t.text is not null
          and base_t.text % :baseTranslationText
      ) base_match
      join key k on k.id = base_match.key_id and k.deleted_at is null
      join project p on p.id = k.project_id and p.base_language_id = base_match.language_id
        and p.deleted_at is null
      join translation_memory_project tmp_w
        on tmp_w.project_id = p.id and tmp_w.write_access = true
      join translation_memory tm_virt
        on tm_virt.id = tmp_w.translation_memory_id and tm_virt.id in :tmIds
      left join namespace ns on ns.id = k.namespace_id
      left join branch b on b.id = k.branch_id
      join translation target_t
        on target_t.key_id = k.id and target_t.language_id <> base_match.language_id
      join language target_lang on target_lang.id = target_t.language_id
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
