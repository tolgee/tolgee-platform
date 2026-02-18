import React from 'react';
import { Box, styled } from '@mui/material';
import { AlertTriangle } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';
import type { QaCheckResultItem } from '../hooks/useQaCheckPreview';

const StyledItem = styled(Box)`
  display: flex;
  align-items: flex-start;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(0.75, 1)};
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.warning.light};
`;

const StyledIcon = styled(Box)`
  display: flex;
  align-items: center;
  color: ${({ theme }) => theme.palette.warning.main};
  flex-shrink: 0;
  margin-top: 2px;
`;

const StyledContent = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
`;

const StyledType = styled(Box)`
  font-size: 13px;
  font-weight: 500;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledMessage = styled(Box)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

function useQaIssueMessage(message: string): string {
  const { t } = useTranslate();
  switch (message) {
    case 'qa_empty_translation':
      return t('qa_issue_empty_translation');
    case 'qa_check_failed':
      return t('qa_check_failed');
    default:
      return message;
  }
}

function useQaCheckTypeLabel(type: string): string {
  const { t } = useTranslate();
  switch (type) {
    case 'EMPTY_TRANSLATION':
      return t('qa_check_type_empty_translation');
    default:
      return type;
  }
}

type Props = {
  issue: QaCheckResultItem;
};

export const QaCheckItem: React.FC<Props> = ({ issue }) => {
  const typeLabel = useQaCheckTypeLabel(issue.type);
  const messageText = useQaIssueMessage(issue.message);

  return (
    <StyledItem data-cy="qa-check-item">
      <StyledIcon>
        <AlertTriangle width={16} height={16} />
      </StyledIcon>
      <StyledContent>
        <StyledType>{typeLabel}</StyledType>
        <StyledMessage>{messageText}</StyledMessage>
      </StyledContent>
    </StyledItem>
  );
};
