import { components } from 'tg.service/apiSchema.generated';
import { AiReviewRuleModel } from 'tg.ee.module/aiReview/utils/types';

type LanguageModel = components['schemas']['LanguageModel'];

export type AiReviewRuleNameSource = Pick<
  AiReviewRuleModel,
  'tagIn' | 'tagNotIn' | 'namespaceIn' | 'namespaceNotIn' | 'languageIds'
> & { name?: string | null };

export type RuleNameTranslate = (
  key: string,
  defaultValue: string,
  params?: Record<string, string | number>
) => string;

const MAX_LISTED_ITEMS = 2;
const SEPARATOR = ' · ';

export const formatAiReviewRuleDisplayName = (
  rule: AiReviewRuleNameSource,
  languages: Pick<LanguageModel, 'id' | 'name'>[],
  t: RuleNameTranslate
) => {
  const name = rule.name?.trim();
  if (name) {
    return name;
  }

  const tagLabel = (tag: string) =>
    tag || t('translations_filters_tags_without_tags', 'Without tags');
  const namespaceLabel = (namespace: string) =>
    namespace ||
    t('translations_filters_namespaces_without_namespace', 'Without namespace');

  const list = (items: string[], unlisted = 0) => {
    const listed = items.slice(0, MAX_LISTED_ITEMS).join(', ');
    const rest = Math.max(items.length - MAX_LISTED_ITEMS, 0) + unlisted;
    if (rest <= 0) {
      return listed;
    }
    return t('ai_review_rule_name_more', '{items} +{count}', {
      items: listed,
      count: rest,
    });
  };

  const parts: string[] = [];
  if (rule.tagIn.length) {
    parts.push(
      t('ai_review_rule_name_tags', 'Tags {tags}', {
        tags: list(rule.tagIn.map(tagLabel)),
      })
    );
  }
  if (rule.tagNotIn.length) {
    parts.push(
      t('ai_review_rule_name_not_tags', 'Not tagged {tags}', {
        tags: list(rule.tagNotIn.map(tagLabel)),
      })
    );
  }
  if (rule.namespaceIn.length) {
    parts.push(
      t('ai_review_rule_name_namespaces', 'Namespace {namespaces}', {
        namespaces: list(rule.namespaceIn.map(namespaceLabel)),
      })
    );
  }
  if (rule.namespaceNotIn.length) {
    parts.push(
      t('ai_review_rule_name_not_namespaces', 'Not in namespace {namespaces}', {
        namespaces: list(rule.namespaceNotIn.map(namespaceLabel)),
      })
    );
  }
  if (!parts.length) {
    parts.push(t('ai_review_rule_scope_all_keys', 'All keys'));
  }

  const languageNames = rule.languageIds
    .map((id) => languages.find((l) => l.id === id)?.name)
    .filter((n): n is string => Boolean(n));
  parts.push(
    rule.languageIds.length
      ? list(languageNames, rule.languageIds.length - languageNames.length)
      : t('ai_review_rule_scope_all_languages', 'All languages')
  );

  return parts.join(SEPARATOR);
};
