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
import { SubfilterAiReviewProps } from 'eeSetup/EeModuleType';
import { components } from 'tg.service/apiSchema.generated';
import { AiReviewBadge } from 'tg.ee.module/aiReview/translations/AiReviewBadge';

type AiReviewStatus =
  components['schemas']['TranslationAiReviewSummaryModel']['status'];

const OUTCOMES: {
  outcome: AiReviewFilterOutcome;
  label: React.ReactElement;
  badgeStatus?: AiReviewStatus;
}[] = [
  {
    outcome: 'REVIEWED_BY_AI',
    label: (
      <T
        keyName="ai_review_filter_reviewed_by_ai"
        defaultValue="Reviewed by AI"
      />
    ),
    badgeStatus: 'APPROVED',
  },
  {
    outcome: 'BELOW_THRESHOLD',
    label: (
      <T
        keyName="ai_review_filter_below_threshold"
        defaultValue="Below threshold"
      />
    ),
    badgeStatus: 'BELOW_THRESHOLD',
  },
  {
    outcome: 'DRY_RUN_PASS',
    label: (
      <T
        keyName="ai_review_filter_dry_run_pass"
        defaultValue="Dry run: would be auto-reviewed"
      />
    ),
    badgeStatus: 'DRY_RUN_PASS',
  },
  {
    outcome: 'DRY_RUN_FAIL',
    label: (
      <T
        keyName="ai_review_filter_dry_run_fail"
        defaultValue="Dry run: would not be auto-reviewed"
      />
    ),
    badgeStatus: 'DRY_RUN_FAIL',
  },
  {
    outcome: 'FAILED',
    label: <T keyName="ai_review_filter_failed" defaultValue="Failed" />,
    badgeStatus: 'FAILED',
  },
  {
    outcome: 'CONFIRMED',
    label: (
      <T
        keyName="ai_review_filter_confirmed"
        defaultValue="Confirmed by a human"
      />
    ),
    badgeStatus: 'CONFIRMED',
  },
  {
    outcome: 'REJECTED',
    label: (
      <T
        keyName="ai_review_filter_rejected"
        defaultValue="Rejected by a human"
      />
    ),
    badgeStatus: 'REJECTED',
  },
  {
    outcome: 'NOT_REVIEWED_YET',
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
            setOpen(false);
          }}
          slotProps={{ paper: { style: { minWidth: 250 } } }}
        >
          <Box display="grid">
            {OUTCOMES.map(({ outcome, label, badgeStatus }) => (
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
            ))}
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
                      data-cy-language={lang.tag}
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
  const first = value.filterAiReviewOutcomes?.[0];
  return OUTCOMES.find(({ outcome }) => outcome === first)?.label;
}
