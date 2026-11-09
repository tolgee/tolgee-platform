import { useTranslate } from '@tolgee/react';

import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useListFormatter } from 'tg.hooks/useLocale';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

type Labels = {
  withoutTagsLabel: string;
  defaultNamespaceLabel: string;
};

export const useAppliesToSummary = (
  values: RuleFormValues,
  { withoutTagsLabel, defaultNamespaceLabel }: Labels
) => {
  const { t } = useTranslate();
  const languages = useProjectLanguages();
  const formatList = useListFormatter();

  const tagLabel = (value: string) => (value === '' ? withoutTagsLabel : value);
  const namespaceLabel = (value: string) =>
    value === '' ? defaultNamespaceLabel : value;
  const branchLabel = (value: string) => value;

  const filters: {
    items: string[];
    label: (value: string) => string;
    text: (list: string) => string;
  }[] = [
    {
      items: values.tagIn,
      label: tagLabel,
      text: (list) =>
        t('ai_review_rule_summary_tags', 'Tags: {list}', { list }),
    },
    {
      items: values.tagNotIn,
      label: tagLabel,
      text: (list) =>
        t('ai_review_rule_summary_tags_not', 'Except tags: {list}', { list }),
    },
    {
      items: values.namespaceIn,
      label: namespaceLabel,
      text: (list) =>
        t('ai_review_rule_summary_namespaces', 'Namespaces: {list}', { list }),
    },
    {
      items: values.namespaceNotIn,
      label: namespaceLabel,
      text: (list) =>
        t(
          'ai_review_rule_summary_namespaces_not',
          'Except namespaces: {list}',
          {
            list,
          }
        ),
    },
    {
      items: values.branchIn,
      label: branchLabel,
      text: (list) =>
        t('ai_review_rule_summary_branches', 'Branches: {list}', { list }),
    },
    {
      items: values.branchNotIn,
      label: branchLabel,
      text: (list) =>
        t('ai_review_rule_summary_branches_not', 'Except branches: {list}', {
          list,
        }),
    },
  ];

  const filterParts = filters
    .filter((filter) => filter.items.length)
    .map((filter) => filter.text(formatList(filter.items.map(filter.label))));

  const selectedLanguages = languages.filter((language) =>
    values.languageIds.includes(language.id)
  );
  const languagesPart = selectedLanguages.length
    ? formatList(selectedLanguages.map((language) => language.name))
    : t('ai_review_rule_form_languages_all', 'All languages');
  const keysPart = filterParts.length
    ? t('ai_review_rule_summary_filtered_keys', 'Filtered keys')
    : t('ai_review_rule_summary_all_keys', 'All keys');

  return {
    primary: t('ai_review_rule_summary_applies', '{keys} • {languages}', {
      keys: keysPart,
      languages: languagesPart,
    }),
    secondaryLines: filterParts.length
      ? filterParts
      : [t('ai_review_rule_summary_no_filters', 'No filters')],
  };
};
