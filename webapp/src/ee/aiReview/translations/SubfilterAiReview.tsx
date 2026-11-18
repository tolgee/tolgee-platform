import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { CompactListSubheader } from 'tg.component/ListComponents';
import {
  AiReviewFilterOutcome,
  aiReviewScoreFilterActive,
  FiltersInternal,
  withoutAiReviewScoreFilter,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterAiReviewProps } from 'eeSetup/EeModuleType';
import { components } from 'tg.service/apiSchema.generated';
import { AiReviewBadge } from 'tg.ee.module/aiReview/translations/AiReviewBadge';
import { ScoreFilterRow } from 'tg.ee.module/aiReview/translations/ScoreFilterRow';

type AiReviewStatus =
  components['schemas']['TranslationAiReviewSummaryModel']['status'];

const OUTCOMES: {
  outcomes: AiReviewFilterOutcome[];
  cyOutcome: string;
  label: React.ReactElement;
  badgeStatus?: AiReviewStatus;
}[] = [
  {
    outcomes: ['REVIEWED_BY_AI'],
    cyOutcome: 'REVIEWED_BY_AI',
    label: (
      <T
        keyName="ai_review_filter_reviewed_by_ai"
        defaultValue="Reviewed by AI"
      />
    ),
    badgeStatus: 'APPROVED',
  },
  {
    outcomes: ['BELOW_THRESHOLD'],
    cyOutcome: 'BELOW_THRESHOLD',
    label: (
      <T
        keyName="ai_review_filter_below_threshold"
        defaultValue="Below threshold"
      />
    ),
    badgeStatus: 'BELOW_THRESHOLD',
  },
  {
    outcomes: ['SCORE_ONLY_PASS', 'SCORE_ONLY_FAIL'],
    cyOutcome: 'SCORE_ONLY',
    label: (
      <T keyName="ai_review_filter_score_only" defaultValue="Score only" />
    ),
    badgeStatus: 'SCORE_ONLY_PASS',
  },
  {
    outcomes: ['FAILED'],
    cyOutcome: 'FAILED',
    label: <T keyName="ai_review_filter_failed" defaultValue="Failed" />,
    badgeStatus: 'FAILED',
  },
  {
    outcomes: ['OVERRIDDEN'],
    cyOutcome: 'OVERRIDDEN',
    label: (
      <T
        keyName="ai_review_filter_overridden"
        defaultValue="Overridden by human"
      />
    ),
    badgeStatus: 'OVERRIDDEN',
  },
  {
    outcomes: ['NOT_REVIEWED_YET'],
    cyOutcome: 'NOT_REVIEWED_YET',
    label: (
      <T
        keyName="ai_review_filter_not_reviewed_yet"
        defaultValue="Not reviewed yet"
      />
    ),
  },
];

export const SubfilterAiReview = ({
  value,
  actions,
  selectedLanguages,
}: SubfilterAiReviewProps) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const [expanded, setExpanded] = useState(
    value.filterAiReviewOutcomeLanguage !== undefined
  );
  const flushScoreDraft = useRef<() => void>();

  function toggleLanguageScope(
    newValue: FiltersInternal['filterAiReviewOutcomeLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterAiReviewOutcomeLanguage:
        newValue === value.filterAiReviewOutcomeLanguage ? undefined : newValue,
    });
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl}
        data-cy-id="ai-review"
        label={t('translations_filters_heading_ai_review', 'AI review')}
        onClick={() => setOpen(true)}
        selected={Boolean(getAiReviewFiltersLength(value))}
        open={open}
      />
      {open && (
        <Menu
          open={open}
          anchorEl={anchorEl.current!}
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          onClose={() => {
            flushScoreDraft.current?.();
            setOpen(false);
          }}
          slotProps={{ paper: { style: { minWidth: 250 } } }}
          // The submenu is a portal, so its key events bubble through the
          // React tree into the parent filter menu, whose MenuList would
          // move focus a second time per key press.
          onKeyDown={(e) => {
            if (e.key !== 'Escape') {
              e.stopPropagation();
            }
          }}
        >
          {OUTCOMES.map(({ outcomes, cyOutcome, label, badgeStatus }) => (
            <FilterItem
              key={cyOutcome}
              label={label}
              selected={outcomes.every((outcome) =>
                Boolean(value.filterAiReviewOutcomes?.includes(outcome))
              )}
              onClick={() =>
                actions.setFilters(toggleAiReviewOutcomes(value, outcomes))
              }
              indicator={
                badgeStatus && (
                  <AiReviewBadge
                    aiReview={{
                      status: badgeStatus,
                      scoreOnlyRun: badgeStatus.startsWith('SCORE_ONLY'),
                      lastFailed: false,
                      queued: false,
                    }}
                  />
                )
              }
              data-cy="ai-review-filter-item"
              data-cy-outcome={cyOutcome}
            />
          ))}
          <CompactListSubheader>
            {t('ai_review_filter_score_subheader', 'Score')}
          </CompactListSubheader>
          <ScoreFilterRow
            value={value}
            setFilters={actions.setFilters}
            flushRef={flushScoreDraft}
          />
          <CompactListSubheader>
            <Box display="flex" justifyContent="space-between">
              <Box>{t('translations_filter_languages_select_title')}</Box>
            </Box>
          </CompactListSubheader>
          <FilterItem
            data-cy="translations-filter-apply-for-all"
            label={t('translations_filter_languages_all')}
            selected={value.filterAiReviewOutcomeLanguage === undefined}
            onClick={() => toggleLanguageScope(undefined)}
            exclusive
          />
          {expanded &&
            selectedLanguages
              ?.filter((lang) => !lang.base)
              .map((lang) => (
                <FilterItem
                  data-cy="translations-filter-apply-for-language"
                  data-cy-language={lang.tag}
                  key={lang.id}
                  label={lang.name}
                  selected={value.filterAiReviewOutcomeLanguage === lang.tag}
                  onClick={() => toggleLanguageScope(lang.tag)}
                  exclusive
                />
              ))}
          <MenuItem
            data-cy="translations-filter-apply-for-expand"
            role="button"
            onClick={() => setExpanded((v) => !v)}
            sx={{
              display: 'flex',
              justifyContent: 'center',
            }}
          >
            {expanded ? <ChevronUp /> : <ChevronDown />}
          </MenuItem>
        </Menu>
      )}
    </>
  );
};

export function toggleAiReviewOutcomes(
  value: FiltersInternal,
  outcomes: AiReviewFilterOutcome[]
): FiltersInternal {
  const selected = value.filterAiReviewOutcomes ?? [];
  const allSelected = outcomes.every((outcome) => selected.includes(outcome));
  const next = allSelected
    ? selected.filter((outcome) => !outcomes.includes(outcome))
    : [
        ...selected,
        ...outcomes.filter((outcome) => !selected.includes(outcome)),
      ];
  const result: FiltersInternal = {
    ...value,
    filterAiReviewOutcomes: next.length > 0 ? next : undefined,
  };
  if (next.includes('NOT_REVIEWED_YET')) {
    return withoutAiReviewScoreFilter(result);
  }
  return result;
}

export function getAiReviewFiltersLength(value: FiltersInternal) {
  const selected = value.filterAiReviewOutcomes ?? [];
  const outcomeCount = OUTCOMES.filter(({ outcomes }) =>
    outcomes.some((outcome) => selected.includes(outcome))
  ).length;
  return outcomeCount + (aiReviewScoreFilterActive(value) ? 1 : 0);
}

export function getAiReviewFiltersName(value: FiltersInternal) {
  const first = value.filterAiReviewOutcomes?.[0];
  const outcomeLabel =
    first !== undefined
      ? OUTCOMES.find(({ outcomes }) => outcomes.includes(first))?.label
      : undefined;
  if (outcomeLabel) return outcomeLabel;
  if (!aiReviewScoreFilterActive(value)) return undefined;
  return (
    <T
      keyName="ai_review_filter_score_chip"
      defaultValue="Score {mode, select, HIGHER_THAN {higher than} LOWER_THAN {lower than} other {equal to}} {score}"
      params={{
        mode: value.filterAiReviewScoreMode!,
        score: value.filterAiReviewScore!,
      }}
    />
  );
}
