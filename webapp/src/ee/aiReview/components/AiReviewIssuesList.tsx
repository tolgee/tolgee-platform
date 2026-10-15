import { Box, styled, Typography, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useAiReviewSeverityLabel } from 'tg.ee.module/aiReview/hooks/useAiReviewLabels';
import {
  AiReviewIssueModel,
  AiReviewIssueSeverity,
} from 'tg.ee.module/aiReview/utils/types';

const StyledList = styled('div')`
  display: grid;
  gap: 10px;
`;

const StyledIssue = styled('div')`
  display: grid;
  gap: 2px;
`;

const StyledHeader = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  min-width: 0;
`;

const StyledSeverity = styled('span')`
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0px 6px;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
`;

const StyledCategory = styled('span')`
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
  color: ${({ theme }) => theme.palette.text.secondary};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
`;

const StyledPoints = styled('span')`
  margin-left: auto;
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
`;

const StyledSuggestion = styled('div')`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
  white-space: pre-wrap;
  word-break: break-word;
`;

type Props = {
  issues: AiReviewIssueModel[];
};

export const AiReviewIssuesList = ({ issues }: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const severityLabel = useAiReviewSeverityLabel();

  const severityColors = (severity: AiReviewIssueSeverity) => {
    const alert = theme.palette.tokens._components.alert;
    switch (severity) {
      case 'CRITICAL':
        return alert.error;
      case 'MAJOR':
        return alert.warning;
      default:
        return {
          background: theme.palette.emphasis[100],
          color: theme.palette.text.primary,
        };
    }
  };

  if (!issues.length) {
    return (
      <Typography variant="body2" color="text.secondary">
        {t('ai_review_no_issues', 'No issues found')}
      </Typography>
    );
  }

  return (
    <StyledList>
      {issues.map((issue, index) => {
        const colors = severityColors(issue.severity);
        return (
          <StyledIssue
            key={index}
            data-cy="ai-review-issue"
            data-cy-severity={issue.severity}
          >
            <StyledHeader>
              <StyledSeverity
                style={{ background: colors.background, color: colors.color }}
              >
                {severityLabel(issue.severity)}
              </StyledSeverity>
              <StyledCategory>{issue.category}</StyledCategory>
              <StyledPoints>−{issue.points}</StyledPoints>
            </StyledHeader>
            <Box sx={{ fontSize: 14, wordBreak: 'break-word' }}>
              {issue.message}
            </Box>
            {issue.suggestion && (
              <StyledSuggestion>
                {t('ai_review_issue_suggestion', 'Suggestion: {suggestion}', {
                  suggestion: issue.suggestion,
                })}
              </StyledSuggestion>
            )}
          </StyledIssue>
        );
      })}
    </StyledList>
  );
};
