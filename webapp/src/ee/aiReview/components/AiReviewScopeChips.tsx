import { Box, styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Folder, GitBranch01, Tag01 } from '@untitled-ui/icons-react';

import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { components } from 'tg.service/apiSchema.generated';
import { AiReviewRuleModel } from 'tg.ee.module/aiReview/utils/types';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledChips = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
`;

const StyledChip = styled('div')`
  display: inline-flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  height: 24px;
  padding: 0px ${({ theme }) => theme.spacing(1)};
  border-radius: 12px;
  font-size: 13px;
  max-width: 220px;
  background: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[100]
      : theme.palette.emphasis[200]};

  & svg {
    width: 14px;
    height: 14px;
    flex-shrink: 0;
    color: ${({ theme }) => theme.palette.text.secondary};
  }

  & .label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &.excluded .label {
    text-decoration: line-through;
  }
`;

type Props = {
  rule: Pick<
    AiReviewRuleModel,
    | 'tagIn'
    | 'tagNotIn'
    | 'namespaceIn'
    | 'namespaceNotIn'
    | 'languageIds'
    | 'branchIn'
    | 'branchNotIn'
  >;
  languages: LanguageModel[];
};

export const AiReviewScopeChips = ({ rule, languages }: Props) => {
  const { t } = useTranslate();

  const tagLabel = (tag: string) =>
    tag || t('translations_filters_tags_without_tags');
  const namespaceLabel = (namespace: string) =>
    namespace || t('translations_filters_namespaces_without_namespace');

  const ruleLanguages = rule.languageIds
    .map((id) => languages.find((l) => l.id === id))
    .filter((l): l is LanguageModel => Boolean(l));

  const keysUnrestricted =
    !rule.tagIn.length &&
    !rule.tagNotIn.length &&
    !rule.namespaceIn.length &&
    !rule.namespaceNotIn.length &&
    !rule.branchIn.length &&
    !rule.branchNotIn.length;

  return (
    <StyledChips data-cy="ai-review-rule-scope">
      {keysUnrestricted && (
        <StyledChip>
          <span className="label">
            {t('ai_review_rule_scope_all_keys', 'All keys')}
          </span>
        </StyledChip>
      )}
      {rule.tagIn.map((tag) => (
        <StyledChip key={`tag-in-${tag}`}>
          <Tag01 />
          <span className="label">{tagLabel(tag)}</span>
        </StyledChip>
      ))}
      {rule.tagNotIn.map((tag) => (
        <Tooltip
          key={`tag-out-${tag}`}
          title={t('ai_review_rule_scope_excluded', 'Excluded')}
          disableInteractive
        >
          <StyledChip className="excluded">
            <Tag01 />
            <span className="label">{tagLabel(tag)}</span>
          </StyledChip>
        </Tooltip>
      ))}
      {rule.namespaceIn.map((ns) => (
        <StyledChip key={`ns-in-${ns}`}>
          <Folder />
          <span className="label">{namespaceLabel(ns)}</span>
        </StyledChip>
      ))}
      {rule.namespaceNotIn.map((ns) => (
        <Tooltip
          key={`ns-out-${ns}`}
          title={t('ai_review_rule_scope_excluded', 'Excluded')}
          disableInteractive
        >
          <StyledChip className="excluded">
            <Folder />
            <span className="label">{namespaceLabel(ns)}</span>
          </StyledChip>
        </Tooltip>
      ))}
      {rule.branchIn.map((branch) => (
        <StyledChip key={`branch-in-${branch}`}>
          <GitBranch01 />
          <span className="label">{branch}</span>
        </StyledChip>
      ))}
      {rule.branchNotIn.map((branch) => (
        <Tooltip
          key={`branch-out-${branch}`}
          title={t('ai_review_rule_scope_excluded', 'Excluded')}
          disableInteractive
        >
          <StyledChip className="excluded">
            <GitBranch01 />
            <span className="label">{branch}</span>
          </StyledChip>
        </Tooltip>
      ))}
      {ruleLanguages.length ? (
        ruleLanguages.map((l) => (
          <Tooltip key={`lang-${l.id}`} title={l.name} disableInteractive>
            <StyledChip>
              <Box display="flex">
                <CircledLanguageIcon size={14} flag={l.flagEmoji} />
              </Box>
              <span className="label">{l.tag}</span>
            </StyledChip>
          </Tooltip>
        ))
      ) : (
        <StyledChip>
          <span className="label">
            {t('ai_review_rule_scope_all_languages', 'All languages')}
          </span>
        </StyledChip>
      )}
    </StyledChips>
  );
};
