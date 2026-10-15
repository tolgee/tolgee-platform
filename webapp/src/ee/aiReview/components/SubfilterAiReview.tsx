import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Divider, Menu, MenuItem } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { CompactListSubheader } from 'tg.component/ListComponents';
import { useProject } from 'tg.hooks/useProject';
import {
  AiReviewFilterOutcome,
  FiltersInternal,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterAiReviewProps } from 'eeSetup/EeModuleType';
import { aiReviewSubmenuProps } from 'tg.ee.module/aiReview/components/AiReviewFilterSubmenuProps';
import { AiReviewScoreBelowSubmenu } from 'tg.ee.module/aiReview/components/AiReviewScoreBelowSubmenu';
import { AiReviewRuleSubmenu } from 'tg.ee.module/aiReview/components/AiReviewRuleSubmenu';
import { useAiReviewRules } from 'tg.ee.module/aiReview/hooks/useAiReviewRules';
import { useAiReviewRuleDisplayName } from 'tg.ee.module/aiReview/hooks/useAiReviewRuleDisplayName';
import { useAiReviewFilterOutcomeLabel } from 'tg.ee.module/aiReview/hooks/useAiReviewLabels';
import { AiReviewOutcomeIndicator } from 'tg.ee.module/aiReview/components/AiReviewOutcomeIndicator';

export const AI_REVIEW_FILTER_OUTCOMES: AiReviewFilterOutcome[] = [
  'AUTO_REVIEWED',
  'BELOW_THRESHOLD',
  'WOULD_AUTO_REVIEW',
  'WOULD_NOT_AUTO_REVIEW',
  'FAILED',
  'NOT_REVIEWED',
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
    value.filterAiReviewApplyLanguage !== undefined
  );
  const outcomeLabel = useAiReviewFilterOutcomeLabel();

  function toggleOutcome(outcome: AiReviewFilterOutcome) {
    if (value.filterAiReviewOutcome?.includes(outcome)) {
      actions.removeFilter('filterAiReviewOutcome', outcome);
    } else {
      actions.addFilter('filterAiReviewOutcome', outcome);
    }
  }

  function toggleLanguage(
    newValue: FiltersInternal['filterAiReviewApplyLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterAiReviewApplyLanguage:
        newValue === value.filterAiReviewApplyLanguage ? undefined : newValue,
    });
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_ai_review', 'AI review')}
        onClick={() => setOpen(true)}
        selected={Boolean(getAiReviewFiltersLength(value))}
        open={open}
        data-cy="translations-filter-ai-review"
      />
      {open && (
        <Menu
          open={open}
          anchorEl={anchorEl.current!}
          {...aiReviewSubmenuProps}
          onClose={() => setOpen(false)}
          slotProps={{ paper: { style: { minWidth: 300 } } }}
        >
          <Box display="grid" data-cy="translations-filter-ai-review-menu">
            {AI_REVIEW_FILTER_OUTCOMES.map((outcome) => (
              <FilterItem
                key={outcome}
                label={outcomeLabel(outcome)}
                selected={Boolean(
                  value.filterAiReviewOutcome?.includes(outcome)
                )}
                onClick={() => toggleOutcome(outcome)}
                indicator={<AiReviewOutcomeIndicator outcome={outcome} />}
                data-cy="translations-filter-ai-review-outcome"
                data-cy-outcome={outcome}
              />
            ))}
            <Divider />
            <AiReviewScoreBelowSubmenu value={value} actions={actions} />
            <AiReviewRuleSubmenu
              value={value}
              actions={actions}
              selectedLanguages={selectedLanguages}
            />
            <CompactListSubheader>
              <Box display="flex" justifyContent="space-between">
                <Box>{t('translations_filter_languages_select_title')}</Box>
              </Box>
            </CompactListSubheader>
            <FilterItem
              data-cy="translations-filter-apply-no-base"
              label={t('translations_filter_languages_no_base')}
              selected={value.filterAiReviewApplyLanguage === undefined}
              onClick={() => toggleLanguage(undefined)}
              exclusive
            />
            {expanded && (
              <>
                <FilterItem
                  data-cy="translations-filter-apply-for-all"
                  label={t('translations_filter_languages_all')}
                  selected={value.filterAiReviewApplyLanguage === true}
                  onClick={() => toggleLanguage(true)}
                  exclusive
                />
                {selectedLanguages?.map((lang) => (
                  <FilterItem
                    data-cy="translations-filter-apply-for-language"
                    data-cy-language={lang.tag}
                    key={lang.id}
                    label={lang.name}
                    selected={value.filterAiReviewApplyLanguage === lang.tag}
                    onClick={() => toggleLanguage(lang.tag)}
                    exclusive
                  />
                ))}
              </>
            )}
            <MenuItem
              data-cy="translations-filter-apply-for-expand"
              role="button"
              onClick={() => setExpanded((v) => !v)}
              sx={{ display: 'flex', justifyContent: 'center' }}
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
  return (
    (value.filterAiReviewOutcome?.length ?? 0) +
    (value.filterAiReviewScoreBelow !== undefined ? 1 : 0) +
    (value.filterAiReviewRuleId?.length ?? 0)
  );
}

const OutcomeFilterName = ({ outcome }: { outcome: AiReviewFilterOutcome }) => {
  const outcomeLabel = useAiReviewFilterOutcomeLabel();
  return (
    <T
      keyName="translations_filter_ai_review_summary"
      defaultValue="AI review: {filter}"
      params={{ filter: outcomeLabel(outcome) }}
    />
  );
};

export function getAiReviewFiltersName(value: FiltersInternal) {
  if (value.filterAiReviewOutcome?.length) {
    return <OutcomeFilterName outcome={value.filterAiReviewOutcome[0]} />;
  }
  if (value.filterAiReviewScoreBelow !== undefined) {
    return (
      <T
        keyName="translations_filter_ai_review_summary_score_below"
        defaultValue="AI review: score below {score}"
        params={{ score: value.filterAiReviewScoreBelow }}
      />
    );
  }
  if (value.filterAiReviewRuleId?.length) {
    return <RuleFilterName ruleId={value.filterAiReviewRuleId[0]} />;
  }
}

const RuleFilterName = ({ ruleId }: { ruleId: number }) => {
  const project = useProject();
  const rulesLoadable = useAiReviewRules(project.id);
  const getRuleDisplayName = useAiReviewRuleDisplayName();
  const rule = rulesLoadable.data?._embedded?.rules?.find(
    (r) => r.id === ruleId
  );
  return (
    <T
      keyName="translations_filter_ai_review_summary"
      defaultValue="AI review: {filter}"
      params={{ filter: rule ? getRuleDisplayName(rule, []) : '' }}
    />
  );
};
