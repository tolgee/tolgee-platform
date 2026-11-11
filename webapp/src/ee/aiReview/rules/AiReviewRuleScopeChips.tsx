import { Box, styled, Tooltip, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

import { components } from 'tg.service/apiSchema.generated';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useListFormatter } from 'tg.hooks/useLocale';

const MAX_FLAGS = 5;

const StyledScopeLine = styled(Typography)`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  column-gap: ${({ theme }) => theme.spacing(0.75)};
  row-gap: ${({ theme }) => theme.spacing(0.5)};
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledFlag = styled(FlagImage)`
  display: block;
  border-radius: ${({ theme }) => theme.shape.borderRadius / 2}px;
`;

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

type Props = {
  rule: AiReviewRuleModel;
};

export const AiReviewRuleScopeChips = ({ rule }: Props) => {
  const { t } = useTranslate();
  const languages = useProjectLanguages();
  const formatList = useListFormatter();

  const tagLabel = (tag: string) =>
    tag === '' ? t('ai_review_chip_without_tags', 'Without tags') : tag;
  const namespaceLabel = (namespace: string) =>
    namespace === ''
      ? t('ai_review_chip_default_namespace', 'Default namespace')
      : namespace;
  const ruleLanguages = rule.languageIds
    .map((id) => languages.find((l) => l.id === id))
    .filter(Boolean) as typeof languages;
  const visibleLanguages =
    ruleLanguages.length > MAX_FLAGS
      ? ruleLanguages.slice(0, MAX_FLAGS - 1)
      : ruleLanguages;
  const hiddenLanguages = ruleLanguages.slice(visibleLanguages.length);

  const scopeParts: {
    key: string;
    scope: string;
    kind: string;
    label: string;
  }[] = [
    ...rule.tagIn.map((tag) => ({
      key: `tag-${tag}`,
      scope: tag,
      kind: 'include',
      label: t('ai_review_scope_tag', 'Tag: {value}', { value: tagLabel(tag) }),
    })),
    ...rule.tagNotIn.map((tag) => ({
      key: `tag-not-${tag}`,
      scope: tag,
      kind: 'exclude',
      label: t('ai_review_scope_tag_except', 'Except tag: {value}', {
        value: tagLabel(tag),
      }),
    })),
    ...rule.namespaceIn.map((namespace) => ({
      key: `ns-${namespace}`,
      scope: namespace,
      kind: 'include',
      label: t('ai_review_scope_namespace', 'Namespace: {value}', {
        value: namespaceLabel(namespace),
      }),
    })),
    ...rule.namespaceNotIn.map((namespace) => ({
      key: `ns-not-${namespace}`,
      scope: namespace,
      kind: 'exclude',
      label: t(
        'ai_review_scope_namespace_except',
        'Except namespace: {value}',
        {
          value: namespaceLabel(namespace),
        }
      ),
    })),
    ...rule.branchIn.map((branch) => ({
      key: `branch-${branch}`,
      scope: branch,
      kind: 'include',
      label: t('ai_review_scope_branch', 'Branch: {value}', { value: branch }),
    })),
    ...rule.branchNotIn.map((branch) => ({
      key: `branch-not-${branch}`,
      scope: branch,
      kind: 'exclude',
      label: t('ai_review_scope_branch_except', 'Except branch: {value}', {
        value: branch,
      }),
    })),
  ];

  return (
    <StyledScopeLine variant="body2" data-cy="ai-review-rule-scope">
      {rule.languageIds.length === 0 ? (
        <span data-cy="ai-review-scope-chip" data-cy-scope="languages">
          {t('ai_review_chip_all_languages', 'All languages')}
        </span>
      ) : (
        <Box
          component="span"
          display="inline-flex"
          alignItems="center"
          gap={0.5}
          data-cy="ai-review-rule-languages"
        >
          {visibleLanguages.map((language) => (
            <Tooltip key={language.id} title={language.name} disableInteractive>
              <span data-cy="ai-review-rule-language-flag">
                <StyledFlag flagEmoji={language.flagEmoji ?? ''} height={16} />
              </span>
            </Tooltip>
          ))}
          {hiddenLanguages.length > 0 && (
            <Tooltip
              title={formatList(hiddenLanguages.map((l) => l.name))}
              disableInteractive
            >
              <span data-cy="ai-review-rule-languages-more">
                {t('ai_review_rule_languages_more', '+{count}', {
                  count: hiddenLanguages.length,
                })}
              </span>
            </Tooltip>
          )}
        </Box>
      )}
      {scopeParts.length === 0 && (
        <span data-cy="ai-review-scope-chip" data-cy-scope="keys">
          <span aria-hidden="true">· </span>
          {t('ai_review_scope_all_keys', 'All keys')}
        </span>
      )}
      {scopeParts.map((part) => (
        <span
          key={part.key}
          data-cy="ai-review-scope-chip"
          data-cy-scope={part.scope}
          data-cy-kind={part.kind}
        >
          <span aria-hidden="true">· </span>
          {part.label}
        </span>
      ))}
    </StyledScopeLine>
  );
};
