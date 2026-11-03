import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { CompactListSubheader } from 'tg.component/ListComponents';
import {
  AiReviewFilterOutcome,
  FiltersInternal,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterAiReviewProps } from '../../../eeSetup/EeModuleType';
import { AiReviewBadge } from './AiReviewBadge';

const OUTCOME_BADGE_STATUS = {
  REVIEWED_BY_AI: 'APPROVED',
  BELOW_THRESHOLD: 'BELOW_THRESHOLD',
  DRY_RUN_PASS: 'DRY_RUN_PASS',
  DRY_RUN_FAIL: 'DRY_RUN_FAIL',
  FAILED: 'FAILED',
  NOT_REVIEWED_YET: undefined,
} as const;

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

  const outcomes: { outcome: AiReviewFilterOutcome; label: string }[] = [
    {
      outcome: 'REVIEWED_BY_AI',
      label: t('ai_review_filter_reviewed_by_ai', 'Reviewed by AI'),
    },
    {
      outcome: 'BELOW_THRESHOLD',
      label: t('ai_review_filter_below_threshold', 'Below threshold'),
    },
    {
      outcome: 'DRY_RUN_PASS',
      label: t(
        'ai_review_filter_dry_run_pass',
        'Dry run: would be auto-reviewed'
      ),
    },
    {
      outcome: 'DRY_RUN_FAIL',
      label: t(
        'ai_review_filter_dry_run_fail',
        'Dry run: would not be auto-reviewed'
      ),
    },
    { outcome: 'FAILED', label: t('ai_review_filter_failed', 'Failed') },
    {
      outcome: 'NOT_REVIEWED_YET',
      label: t('ai_review_filter_not_reviewed_yet', 'Not reviewed yet'),
    },
  ];

  function handleToggleOutcome(outcome: AiReviewFilterOutcome) {
    if (value.filterAiReviewOutcomes?.includes(outcome)) {
      actions.removeFilter('filterAiReviewOutcomes', outcome);
    } else {
      actions.addFilter('filterAiReviewOutcomes', outcome);
    }
  }

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
            setOpen(false);
          }}
          slotProps={{ paper: { style: { minWidth: 250 } } }}
        >
          <Box display="grid">
            {outcomes.map(({ outcome, label }) => {
              const badgeStatus = OUTCOME_BADGE_STATUS[outcome];
              return (
                <FilterItem
                  key={outcome}
                  label={label}
                  selected={Boolean(
                    value.filterAiReviewOutcomes?.includes(outcome)
                  )}
                  onClick={() => handleToggleOutcome(outcome)}
                  indicator={
                    badgeStatus && (
                      <AiReviewBadge
                        aiReview={{
                          status: badgeStatus,
                          dryRun: badgeStatus.startsWith('DRY_RUN'),
                          lastFailed: false,
                        }}
                      />
                    )
                  }
                  data-cy="ai-review-filter-item"
                  data-cy-outcome={outcome}
                />
              );
            })}
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
            {expanded && (
              <>
                <FilterItem
                  data-cy="translations-filter-apply-no-base"
                  label={t('translations_filter_languages_no_base')}
                  selected={value.filterAiReviewOutcomeLanguage === false}
                  onClick={() => toggleLanguageScope(false)}
                  exclusive
                />
                {selectedLanguages
                  ?.filter((lang) => !lang.base)
                  .map((lang) => (
                    <FilterItem
                      data-cy="translations-filter-apply-for-language"
                      key={lang.id}
                      label={lang.name}
                      selected={
                        value.filterAiReviewOutcomeLanguage === lang.tag
                      }
                      onClick={() => toggleLanguageScope(lang.tag)}
                      exclusive
                    />
                  ))}
              </>
            )}
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
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getAiReviewFiltersLength(value: FiltersInternal) {
  return value.filterAiReviewOutcomes?.length ?? 0;
}

export function getAiReviewFiltersName(value: FiltersInternal) {
  if (value.filterAiReviewOutcomes?.length) {
    switch (value.filterAiReviewOutcomes[0]) {
      case 'REVIEWED_BY_AI':
        return <T keyName="ai_review_filter_reviewed_by_ai" />;
      case 'BELOW_THRESHOLD':
        return <T keyName="ai_review_filter_below_threshold" />;
      case 'DRY_RUN_PASS':
        return <T keyName="ai_review_filter_dry_run_pass" />;
      case 'DRY_RUN_FAIL':
        return <T keyName="ai_review_filter_dry_run_fail" />;
      case 'FAILED':
        return <T keyName="ai_review_filter_failed" />;
      case 'NOT_REVIEWED_YET':
        return <T keyName="ai_review_filter_not_reviewed_yet" />;
    }
  }
}
