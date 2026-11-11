import { Box, Chip, Typography, styled, useTheme } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';

type AiReviewIssueModel = components['schemas']['AiReviewIssueModel'];

const StyledIssue = styled(Box)`
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
`;

type Props = {
  issues: AiReviewIssueModel[];
};

export const AiReviewIssueList = ({ issues }: Props) => {
  const theme = useTheme();

  function severityColor(severity: string) {
    switch (severity.toLowerCase()) {
      case 'critical':
        return theme.palette.error.main;
      case 'major':
        return theme.palette.warning.main;
      case 'minor':
        return theme.palette.info.main;
      default:
        return theme.palette.emphasis[400];
    }
  }

  return (
    <Box display="grid" gap={1}>
      {issues.map((issue, index) => (
        <StyledIssue
          key={index}
          data-cy="ai-review-issue"
          data-cy-severity={issue.severity.toLowerCase()}
          data-cy-points={String(issue.points)}
        >
          <Chip
            size="small"
            label={issue.severity}
            sx={{
              backgroundColor: severityColor(issue.severity),
              color: theme.palette.getContrastText(
                severityColor(issue.severity)
              ),
            }}
          />
          <Typography variant="body2">{issue.category}</Typography>
          <Typography variant="body2" fontWeight={500}>
            {`−${issue.points}`}
          </Typography>
          <Typography
            variant="body2"
            color="textSecondary"
            sx={{ gridColumn: '1 / -1' }}
          >
            {issue.message}
          </Typography>
        </StyledIssue>
      ))}
    </Box>
  );
};
