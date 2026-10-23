-- Stamps 'language' on TranslationComment activity logged before the entity described it.
UPDATE activity_modified_entity me
SET describing_relations = me.describing_relations || jsonb_build_object('language', de.describing_relations -> 'language')
FROM activity_describing_entity de
WHERE me.entity_class = 'TranslationComment'
  AND NOT jsonb_exists(me.describing_relations, 'language')
  AND de.activity_revision_id = me.activity_revision_id
  AND de.entity_class = 'Translation'
  AND de.entity_id = (me.describing_relations -> 'translation' ->> 'entityId')::bigint
  AND jsonb_exists(de.describing_relations, 'language');
