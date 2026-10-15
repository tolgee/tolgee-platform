import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterAiReviewProps } from 'eeSetup/EeModuleType';
import { aiReviewSubmenuProps } from 'tg.ee.module/aiReview/components/AiReviewFilterSubmenuProps';

export const AI_REVIEW_SCORE_BELOW_OPTIONS = [50, 60, 70, 80, 85, 90, 95];

type Props = Pick<SubfilterAiReviewProps, 'value' | 'actions'>;

export const AiReviewScoreBelowSubmenu = ({ value, actions }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  function toggleScoreBelow(score: number) {
    actions.setFilters({
      ...value,
      filterAiReviewScoreBelow:
        value.filterAiReviewScoreBelow === score ? undefined : score,
    });
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filter_ai_review_score_below', 'Score below…')}
        onClick={() => setOpen(true)}
        selected={value.filterAiReviewScoreBelow !== undefined}
        open={open}
        data-cy="translations-filter-ai-review-score-below"
      />
      {open && (
        <Menu
          open
          anchorEl={anchorEl.current!}
          {...aiReviewSubmenuProps}
          onClose={() => setOpen(false)}
          slotProps={{ paper: { style: { minWidth: 200 } } }}
        >
          <Box
            display="grid"
            data-cy="translations-filter-ai-review-score-menu"
          >
            {AI_REVIEW_SCORE_BELOW_OPTIONS.map((score) => (
              <FilterItem
                key={score}
                label={t(
                  'translations_filter_ai_review_score_below_option',
                  'Below {score}',
                  { score }
                )}
                selected={value.filterAiReviewScoreBelow === score}
                onClick={() => toggleScoreBelow(score)}
                exclusive
                data-cy="translations-filter-ai-review-score-option"
                data-cy-score={score}
              />
            ))}
          </Box>
        </Menu>
      )}
    </>
  );
};
