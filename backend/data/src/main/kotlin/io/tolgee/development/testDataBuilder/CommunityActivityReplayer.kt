package io.tolgee.development.testDataBuilder

import jakarta.persistence.EntityManager

/** Logs ActivityService-shaped activity for the stored translations, suggestions and comments; call inside one transaction, after saving. */
object CommunityActivityReplayer {
  fun replay(
    entityManager: EntityManager,
    projectIds: Collection<Long>,
    maintainerId: Long,
  ) {
    val ids = projectIds.joinToString(",")
    val ref = { cls: String, idColumn: String -> "jsonb_build_object('entityClass', '$cls', 'entityId', $idColumn)" }
    val keyRef = ref("Key", "a.key_id")
    val languageRef = ref("Language", "a.language_id")
    val translationRef = ref("Translation", "a.translation_id")
    val keyAndLanguage = "jsonb_build_object('key', $keyRef, 'language', $languageRef)"
    listOf(
      """
      create temp table seed_activity on commit drop as
      select nextval('activity_sequence') as rev, 'SET_TRANSLATIONS' as type, k.project_id, $maintainerId as author_id,
             tr.created_at as occurred_at, 'Translation' as entity_class, tr.id as entity_id, 0 as revision_type,
             jsonb_build_object('text', jsonb_build_object('old', null, 'new', tr.text)) as modifications,
             tr.key_id, tr.language_id, null::bigint as translation_id
      from translation tr join key k on k.id = tr.key_id
      where k.project_id in ($ids)
      union all
      select nextval('activity_sequence'), 'CREATE_SUGGESTION', s.project_id, s.author_id, s.created_at,
             'TranslationSuggestion', s.id, 0,
             jsonb_build_object('translation', jsonb_build_object('old', null, 'new', s.translation),
                                'state', jsonb_build_object('old', null, 'new', 'ACTIVE')),
             s.key_id, s.language_id, null
      from translation_suggestion s
      where s.project_id in ($ids)
      union all
      select nextval('activity_sequence'),
             case s.state when 'ACCEPTED' then 'ACCEPT_SUGGESTION' else 'DECLINE_SUGGESTION' end,
             s.project_id, $maintainerId, s.updated_at, 'TranslationSuggestion', s.id, 1,
             jsonb_build_object('state', jsonb_build_object('old', 'ACTIVE', 'new', s.state)),
             s.key_id, s.language_id, null
      from translation_suggestion s
      where s.project_id in ($ids) and s.state in ('ACCEPTED', 'DECLINED')
      union all
      select nextval('activity_sequence'), 'TRANSLATION_COMMENT_ADD', k.project_id, c.author_id, c.created_at,
             'TranslationComment', c.id, 0,
             jsonb_build_object('text', jsonb_build_object('old', null, 'new', c.text)),
             tr.key_id, tr.language_id, tr.id
      from translation_comment c join translation tr on tr.id = c.translation_id join key k on k.id = tr.key_id
      where k.project_id in ($ids)
      """,
      """
      insert into activity_revision (id, project_id, author_id, type, "timestamp")
      select rev, project_id, author_id, type, occurred_at from seed_activity
      """,
      """
      insert into activity_modified_entity
        (activity_revision_id, entity_class, entity_id, revision_type, modifications, describing_relations)
      select a.rev, a.entity_class, a.entity_id, a.revision_type, a.modifications,
             case when a.translation_id is null then $keyAndLanguage
                  else jsonb_build_object('translation', $translationRef, 'language', $languageRef) end
      from seed_activity a
      """,
      """
      insert into activity_describing_entity (activity_revision_id, entity_class, entity_id, data, describing_relations)
      select a.rev, 'Translation', tr.id, jsonb_build_object('text', tr.text), $keyAndLanguage
      from seed_activity a join translation tr on tr.id = a.translation_id
      union all
      select a.rev, 'Key', k.id, jsonb_build_object('name', k.name), '{}'
      from seed_activity a join key k on k.id = a.key_id
      union all
      select a.rev, 'Language', l.id, jsonb_build_object('tag', l.tag, 'name', l.name, 'flagEmoji', l.flag_emoji), '{}'
      from seed_activity a join language l on l.id = a.language_id
      """,
    ).forEach { entityManager.createNativeQuery(it).executeUpdate() }
  }
}
