import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { useProject } from 'tg.hooks/useProject';
import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterAiReviewProps } from 'eeSetup/EeModuleType';
import { useAiReviewRules } from 'tg.ee.module/aiReview/hooks/useAiReviewRules';
import { useAiReviewRuleDisplayName } from 'tg.ee.module/aiReview/hooks/useAiReviewRuleDisplayName';
import { aiReviewSubmenuProps } from 'tg.ee.module/aiReview/components/AiReviewFilterSubmenuProps';

export const AiReviewRuleSubmenu = ({
  value,
  actions,
  selectedLanguages,
}: SubfilterAiReviewProps) => {
  const { t } = useTranslate();
  const project = useProject();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const rulesLoadable = useAiReviewRules(project.id);
  const getRuleDisplayName = useAiReviewRuleDisplayName();
  const rules = rulesLoadable.data?._embedded?.rules ?? [];

  function toggleRule(ruleId: number) {
    if (value.filterAiReviewRuleId?.includes(ruleId)) {
      actions.removeFilter('filterAiReviewRuleId', ruleId);
    } else {
      actions.addFilter('filterAiReviewRuleId', ruleId);
    }
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filter_ai_review_by_rule', 'By rule')}
        onClick={() => setOpen(true)}
        selected={Boolean(value.filterAiReviewRuleId?.length)}
        open={open}
        data-cy="translations-filter-ai-review-by-rule"
      />
      {open && (
        <Menu
          open
          anchorEl={anchorEl.current!}
          {...aiReviewSubmenuProps}
          onClose={() => setOpen(false)}
          slotProps={{ paper: { style: { minWidth: 250, maxWidth: 420 } } }}
        >
          <Box display="grid" data-cy="translations-filter-ai-review-rule-menu">
            {rules.length === 0 && (
              <MenuItem disabled>
                {t('translations_filter_ai_review_no_rules', 'No rules yet')}
              </MenuItem>
            )}
            {rules.map((rule) => (
              <FilterItem
                key={rule.id}
                label={
                  <Box
                    component="span"
                    display="block"
                    overflow="hidden"
                    textOverflow="ellipsis"
                    whiteSpace="nowrap"
                  >
                    {getRuleDisplayName(rule, selectedLanguages ?? [])}
                  </Box>
                }
                selected={Boolean(
                  value.filterAiReviewRuleId?.includes(rule.id)
                )}
                onClick={() => toggleRule(rule.id)}
                data-cy="translations-filter-ai-review-rule"
                data-cy-rule={rule.name ?? ''}
              />
            ))}
          </Box>
        </Menu>
      )}
    </>
  );
};
