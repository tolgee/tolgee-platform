import React from 'react';
import {
  Box,
  Button,
  Card,
  IconButton,
  styled,
  Typography,
} from '@mui/material';
import { Check, XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { QaCheck } from 'tg.component/CustomIcons';
import type { QaCheckResultItem } from '../hooks/useQaCheckPreview';
import { useQaIssueMessage } from '../hooks/useQaIssueMessage';
import { useQaCheckTypeLabel } from '../hooks/useQaCheckTypeLabel';

const StyledItem = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(1)};
  margin: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledHeader = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
`;

const StyledIndexCircle = styled(Box)`
  display: flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border: 1px solid ${({ theme }) => theme.palette.primary.main};
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.palette.primary.main};
  flex-shrink: 0;
`;

const StyledIcon = styled(Box)`
  display: flex;
  align-items: center;
  color: ${({ theme }) => theme.palette.text.primary};
  flex-shrink: 0;
`;

const StyledContent = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: 0;
  min-width: 0;
`;

const StyledMessage = styled(Box)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledDiffCard = styled(Card)`
  display: flex;
  align-items: center;
  padding: ${({ theme }) => theme.spacing(1.5)};
  background-color: ${({ theme }) => theme.palette.tokens.text._states.hover};
`;

const StyledDiffText = styled('span')`
  flex: 1;
  font-size: 14px;
  overflow-wrap: break-word;
  min-width: 0;
`;

const StyledUnchanged = styled('span')`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledRemoved = styled('span')`
  color: ${({ theme }) => theme.palette.text.secondary};
  text-decoration: line-through;
`;

const StyledAdded = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledDiffActions = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  flex-shrink: 0;
  margin-left: ${({ theme }) => theme.spacing(1)};
`;

const StyledNormalActions = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
`;

function renderDiff(
  text: string,
  positionStart: number,
  positionEnd: number,
  replacement: string
) {
  const before = text.slice(0, positionStart);
  const removed = text.slice(positionStart, positionEnd);
  const after = text.slice(positionEnd);
  return (
    <>
      {before && <StyledUnchanged>{before}</StyledUnchanged>}
      {removed && <StyledRemoved>{removed}</StyledRemoved>}
      <StyledAdded>{replacement}</StyledAdded>
      {after && <StyledUnchanged>{after}</StyledUnchanged>}
    </>
  );
}

type Props = {
  issue: QaCheckResultItem;
  index?: number;
  text: string;
  slim?: boolean;
};

export const QaCheckItem: React.FC<Props> = ({
  issue,
  index,
  text,
  slim = false,
}) => {
  const typeLabel = useQaCheckTypeLabel(issue.type);
  const messageText = useQaIssueMessage(issue.message, issue.params);
  const hasReplacement = issue.replacement != null;

  return (
    <StyledItem data-cy="qa-check-item">
      <StyledHeader>
        {index !== undefined ? (
          <StyledIndexCircle>{index}</StyledIndexCircle>
        ) : (
          <StyledIcon>
            <QaCheck width={24} height={24} />
          </StyledIcon>
        )}
        <StyledContent>
          <Typography variant="body2">{typeLabel}</Typography>
          <StyledMessage>{messageText}</StyledMessage>
        </StyledContent>
      </StyledHeader>

      {hasReplacement && (
        <StyledDiffCard elevation={0}>
          <StyledDiffText>
            {renderDiff(
              text,
              issue.positionStart,
              issue.positionEnd,
              issue.replacement!
            )}
          </StyledDiffText>
          {slim && (
            <StyledDiffActions>
              <IconButton size="small" color="primary">
                <Check width={20} height={20} />
              </IconButton>
              <IconButton size="small">
                <XClose width={20} height={20} />
              </IconButton>
            </StyledDiffActions>
          )}
        </StyledDiffCard>
      )}

      {(!slim || !hasReplacement) && (
        <StyledNormalActions>
          {hasReplacement && (
            <Button variant="outlined" color="primary" size="small">
              <T keyName="qa_check_action_correct" />
            </Button>
          )}
          <Button variant="text" color="inherit" size="small">
            <T keyName="qa_check_action_ignore" />
          </Button>
        </StyledNormalActions>
      )}
    </StyledItem>
  );
};
