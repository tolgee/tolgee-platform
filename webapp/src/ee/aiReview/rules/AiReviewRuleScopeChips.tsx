import { Box, Chip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

type Props = {
  rule: AiReviewRuleModel;
};

export const AiReviewRuleScopeChips = ({ rule }: Props) => {
  const { t } = useTranslate();
  const languages = useProjectLanguages();

  const tagLabel = (tag: string) =>
    tag === '' ? t('ai_review_chip_without_tags', 'Without tags') : tag;
  const namespaceLabel = (namespace: string) =>
    namespace === ''
      ? t('ai_review_chip_default_namespace', 'Default namespace')
      : namespace;
  const excluded = (label: string) =>
    t('ai_review_chip_except', 'except {value}', { value: label });

  const languagesLabel =
    rule.languageIds.length === 0
      ? t('ai_review_chip_all_languages', 'All languages')
      : rule.languageIds
          .map(
            (id) => languages.find((l) => l.id === id)?.name ?? id.toString()
          )
          .join(', ');

  const chips: {
    key: string;
    scope: string;
    label: string;
    kind: string;
    value?: string;
  }[] = [
    ...rule.tagIn.map((tag) => ({
      key: `tag-${tag}`,
      scope: tag,
      label: tagLabel(tag),
      kind: 'include',
    })),
    ...rule.tagNotIn.map((tag) => ({
      key: `tag-not-${tag}`,
      scope: tag,
      label: excluded(tagLabel(tag)),
      kind: 'exclude',
    })),
    ...rule.namespaceIn.map((namespace) => ({
      key: `ns-${namespace}`,
      scope: namespace,
      label: namespaceLabel(namespace),
      kind: 'include',
    })),
    ...rule.namespaceNotIn.map((namespace) => ({
      key: `ns-not-${namespace}`,
      scope: namespace,
      label: excluded(namespaceLabel(namespace)),
      kind: 'exclude',
    })),
    ...rule.branchIn.map((branch) => ({
      key: `branch-${branch}`,
      scope: branch,
      label: branch,
      kind: 'include',
    })),
    ...rule.branchNotIn.map((branch) => ({
      key: `branch-not-${branch}`,
      scope: branch,
      label: excluded(branch),
      kind: 'exclude',
    })),
    {
      key: 'languages',
      scope: 'languages',
      label: languagesLabel,
      kind: 'include',
      value: languagesLabel,
    },
  ];

  return (
    <Box display="flex" flexWrap="wrap" gap={0.5}>
      {chips.map((chip) => (
        <Chip
          key={chip.key}
          size="small"
          label={chip.label}
          data-cy="ai-review-scope-chip"
          data-cy-scope={chip.scope}
          data-cy-kind={chip.kind}
          data-cy-value={chip.value}
        />
      ))}
    </Box>
  );
};
