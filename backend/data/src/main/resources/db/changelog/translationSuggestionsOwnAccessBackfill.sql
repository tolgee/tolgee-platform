-- Scopes whose expansion contained TRANSLATIONS_VIEW when this changeSet shipped. Frozen with it: do not add
-- scopes introduced later, they have no pre-existing rows to backfill.
UPDATE permission
SET scopes = array_append(scopes, 'TRANSLATION_SUGGESTIONS_OWN_ACCESS'::varchar)
WHERE scopes && ARRAY[
    'ADMIN',
    'ALL_VIEW',
    'BATCH_MACHINE_TRANSLATE',
    'BATCH_PRE_TRANSLATE_BY_TM',
    'BRANCH_PROTECTED_MODIFY',
    'PROMPTS_EDIT',
    'TASKS_EDIT',
    'TASKS_VIEW',
    'TRANSLATIONS_COMMENTS_ADD',
    'TRANSLATIONS_COMMENTS_EDIT',
    'TRANSLATIONS_COMMENTS_SET_STATE',
    'TRANSLATIONS_EDIT',
    'TRANSLATIONS_STATE_EDIT',
    'TRANSLATIONS_SUGGEST',
    'TRANSLATIONS_VIEW',
    'TRANSLATION_LABEL_ASSIGN',
    'TRANSLATION_LABEL_MANAGE',
    'TRANSLATION_SUGGESTIONS_MANAGE'
  ]::varchar[]
  AND NOT scopes @> ARRAY['TRANSLATION_SUGGESTIONS_OWN_ACCESS']::varchar[];
