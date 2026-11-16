import { useLayoutEffect, useRef, useState } from 'react';
import { Box, styled, Tooltip, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

import { components } from 'tg.service/apiSchema.generated';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useListFormatter } from 'tg.hooks/useLocale';

const MAX_FLAGS = 5;
// One filter is shown by name; two or more as "N filters" (listed in the tooltip).
// A single filter that does not fit on the line is shown as "1 filter" too.
const MAX_FILTERS = 1;

const StyledScopeLine = styled(Typography)`
  display: flex;
  align-items: center;
  column-gap: ${({ theme }) => theme.spacing(0.75)};
  /* Takes the free space of the row, so its width does not depend on what it shows;
     below 160px the counts wrap to the next line instead. */
  flex: 1 1 0;
  min-width: 160px;
  position: relative;
  overflow: hidden;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

// The full filter list, invisible, only to measure whether it would fit.
const StyledMeasure = styled('span')`
  position: absolute;
  visibility: hidden;
  pointer-events: none;
  white-space: nowrap;
  display: inline-flex;
  column-gap: ${({ theme }) => theme.spacing(0.75)};
`;

const StyledFlag = styled(FlagImage)`
  display: block;
  border-radius: ${({ theme }) => theme.shape.borderRadius / 2}px;
`;

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];

type Props = {
  rule: AiReviewRuleModel;
  promptName?: string;
};

export const AiReviewRuleScopeChips = ({ rule, promptName }: Props) => {
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

  const promptLabel = promptName
    ? t('ai_review_scope_prompt', 'Prompt: {name}', { name: promptName })
    : undefined;

  // Collapse to "N filters" when there are too many, or when they would not fit on one line.
  // The fit is measured on a hidden copy of the full list against the line's own width,
  // which does not change with what is shown, so the line never switches back and forth.
  const lineRef = useRef<HTMLElement>(null);
  const languagesRef = useRef<HTMLElement>(null);
  const measureRef = useRef<HTMLSpanElement>(null);
  const [overflow, setOverflow] = useState(false);
  const compact =
    scopeParts.length > 0 && (scopeParts.length > MAX_FILTERS || overflow);
  useLayoutEffect(() => {
    const line = lineRef.current;
    if (!line) {
      return;
    }
    const check = () => {
      const languages = languagesRef.current?.offsetWidth ?? 0;
      const filters = measureRef.current?.scrollWidth ?? 0;
      const gap = 6;
      setOverflow(languages + gap + filters > line.clientWidth);
    };
    check();
    const observer = new ResizeObserver(check);
    observer.observe(line);
    return () => observer.disconnect();
  }, [scopeParts.length, promptName]);

  const fullList = (
    <>
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
      {promptLabel && (
        <span data-cy="ai-review-rule-prompt">
          <span aria-hidden="true">· </span>
          {promptLabel}
        </span>
      )}
    </>
  );

  return (
    <StyledScopeLine
      ref={lineRef}
      variant="body2"
      data-cy="ai-review-rule-scope"
      data-cy-compact={String(compact)}
    >
      {rule.languageIds.length === 0 ? (
        <span
          ref={languagesRef}
          data-cy="ai-review-scope-chip"
          data-cy-scope="languages"
        >
          {t('ai_review_chip_all_languages', 'All languages')}
        </span>
      ) : (
        <Box
          ref={languagesRef}
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
      {compact ? (
        <Tooltip
          title={
            <Box display="grid" data-cy="ai-review-rule-filters-tooltip">
              {scopeParts.map((part) => (
                <span
                  key={part.key}
                  data-cy="ai-review-scope-chip"
                  data-cy-scope={part.scope}
                  data-cy-kind={part.kind}
                >
                  {part.label}
                </span>
              ))}
              {promptLabel && <span>{promptLabel}</span>}
            </Box>
          }
          disableInteractive
        >
          <span
            style={{ cursor: 'default' }}
            data-cy="ai-review-rule-filters-count"
          >
            <span aria-hidden="true">· </span>
            {t(
              'ai_review_scope_filters_count',
              '{count, plural, one {# filter} other {# filters}}',
              { count: scopeParts.length }
            )}
          </span>
        </Tooltip>
      ) : (
        fullList
      )}
      {scopeParts.length > 0 && (
        <StyledMeasure ref={measureRef} aria-hidden="true">
          {scopeParts.map((part) => (
            <span key={part.key}>· {part.label}</span>
          ))}
          {promptLabel && <span>· {promptLabel}</span>}
        </StyledMeasure>
      )}
    </StyledScopeLine>
  );
};
