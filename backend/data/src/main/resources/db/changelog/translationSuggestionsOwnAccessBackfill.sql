UPDATE permission
SET scopes = array_append(scopes, 'TRANSLATION_SUGGESTIONS_OWN_ACCESS'::varchar)
WHERE cardinality(scopes) > 0
  AND NOT scopes @> ARRAY['TRANSLATION_SUGGESTIONS_OWN_ACCESS']::varchar[];
