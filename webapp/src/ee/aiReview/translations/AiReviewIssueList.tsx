import { Box, Typography, styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { AiReviewSeverityBadge } from 'tg.ee.module/aiReview/translations/AiReviewSeverityBadge';

type AiReviewIssueModel = components['schemas']['AiReviewIssueModel'];

// Category first, then how serious it is, then the points it cost; the explanation below.
const StyledIssue = styled(Box)`
  display: grid;
  grid-template-columns: auto 1fr auto;
  column-gap: ${({ theme }) => theme.spacing(1)};
  row-gap: ${({ theme }) => theme.spacing(0.5)};
  align-items: center;
`;

const capitalize = (text: string) =>
  text.charAt(0).toUpperCase() + text.slice(1);

type Props = {
  issues: AiReviewIssueModel[];
};

export const AiReviewIssueList = ({ issues }: Props) => {
  return (
    <Box display="grid" gap={1.5}>
      {issues.map((issue, index) => (
        <StyledIssue
          key={index}
          data-cy="ai-review-issue"
          data-cy-severity={issue.severity.toLowerCase()}
          data-cy-points={String(issue.points)}
        >
          {/* Name 15px on the badge's 22px line; points and the explanation 13px, the explanation in the main text colour. */}
          <Typography
            fontWeight={500}
            sx={{ fontSize: 15, lineHeight: '22px' }}
          >
            {capitalize(issue.category ?? '')}
          </Typography>
          <Box>
            <AiReviewSeverityBadge severity={issue.severity} />
          </Box>
          <Typography
            color="textSecondary"
            sx={{
              fontSize: 13,
              lineHeight: '18px',
              fontVariantNumeric: 'tabular-nums',
            }}
          >
            {`−${issue.points}`}
          </Typography>
          <Typography
            color="text.primary"
            sx={{ fontSize: 13, lineHeight: '18px', gridColumn: '1 / -1' }}
          >
            {issue.message}
          </Typography>
        </StyledIssue>
      ))}
    </Box>
  );
};
