import { Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { AiReviewIssueList } from 'tg.ee.module/aiReview/translations/AiReviewIssueList';

type AiReviewIssueModel = components['schemas']['AiReviewIssueModel'];

type Props = {
  issues: AiReviewIssueModel[];
};

// The point deductions of an AI review: "Issue" or "Issues" with the total from two, then the list.
// Used by the AI review card and the review prompt preview.
export const AiReviewIssues = ({ issues }: Props) => {
  const { t } = useTranslate();
  const pointsOff = issues.reduce((sum, issue) => sum + issue.points, 0);

  return (
    <Box display="grid" gap={1.5}>
      {/* The heading sits a bit closer to the issues (8px) than the issues to each other (12px). */}
      <Box
        display="flex"
        justifyContent="space-between"
        color="text.secondary"
        sx={{ fontSize: 13, lineHeight: '18px', mb: -0.5 }}
      >
        <span>
          {t(
            'ai_review_popover_issues_heading',
            '{count, plural, one {Issue} other {Issues}}',
            { count: issues.length }
          )}
        </span>
        {issues.length > 1 && (
          <span
            style={{ fontVariantNumeric: 'tabular-nums' }}
            data-cy="ai-review-popover-points-off"
          >
            {t(
              'ai_review_popover_points_off',
              '{points, plural, one {# point off} other {# points off}}',
              { points: pointsOff }
            )}
          </span>
        )}
      </Box>
      <AiReviewIssueList issues={issues} />
    </Box>
  );
};
