import {
  Box,
  Button,
  IconButton,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import { Check, XClose } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { QaCheck } from 'tg.component/CustomIcons';
import { useQaIssueMessage } from '../hooks/useQaIssueMessage';
import { useQaCheckTypeLabel } from '../hooks/useQaCheckTypeLabel';
import { QaPreviewIssue } from 'tg.ee.module/qa/models/QaPreviewWsModels';
import { TextBlock } from 'tg.component/common/TextBlock';

const StyledItem = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(1)};
  margin: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledIgnoredItem = styled(StyledItem)`
  opacity: 0.5;
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
  min-width: 0;
  flex: 1;
`;

const StyledMessage = styled(Box)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledDiffText = styled('span')`
  font-size: 14px;
  overflow-wrap: break-word;
  white-space: pre-wrap;
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

const StyledAddedSpaces = styled('span')`
  background-color: ${({ theme }) => theme.palette.success.light};
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
  const isReplacementSpacesOnly = /^\s+$/.test(replacement);
  return (
    <>
      {before && <StyledUnchanged>{before}</StyledUnchanged>}
      {removed && <StyledRemoved>{removed}</StyledRemoved>}
      {isReplacementSpacesOnly ? (
        <StyledAddedSpaces>{replacement}</StyledAddedSpaces>
      ) : (
        <StyledAdded>{replacement}</StyledAdded>
      )}
      {after && <StyledUnchanged>{after}</StyledUnchanged>}
    </>
  );
}

type Props = {
  issue: QaPreviewIssue;
  index?: number;
  text: string;
  slim?: boolean;
  onCorrect?: () => void;
  onIgnore?: () => void;
};

export const QaCheckItem = ({
  issue,
  index,
  text,
  slim = false,
  onCorrect,
  onIgnore,
}: Props) => {
  const { t } = useTranslate();
  const typeLabel = useQaCheckTypeLabel(issue.type);
  const messageText = useQaIssueMessage(issue.message, issue.params);
  const hasReplacement = issue.replacement != null;

  const showDiff = hasReplacement && issue.state === 'OPEN';
  const showButtonRow = !slim;

  const Container = issue.state === 'IGNORED' ? StyledIgnoredItem : StyledItem;

  const buttonCorrectSmall = onCorrect && (
    <Tooltip title={t('qa_check_action_correct')}>
      <IconButton size="small" color="primary" onClick={onCorrect}>
        <Check width={20} height={20} />
      </IconButton>
    </Tooltip>
  );

  const buttonIgnoreSmall = onIgnore && (
    <Tooltip title={t('qa_check_action_ignore')}>
      <IconButton size="small" onClick={onIgnore}>
        <XClose width={20} height={20} />
      </IconButton>
    </Tooltip>
  );

  const buttonCorrectBig = onCorrect && (
    <Button variant="outlined" color="primary" size="small" onClick={onCorrect}>
      <T keyName="qa_check_action_correct" />
    </Button>
  );

  const buttonIgnoreBig = onIgnore && (
    <Button variant="text" color="inherit" size="small" onClick={onIgnore}>
      {issue.state === 'IGNORED' ? (
        <T keyName="qa_check_action_unignore" />
      ) : (
        <T keyName="qa_check_action_ignore" />
      )}
    </Button>
  );

  const textActions = !showButtonRow ? (
    <>
      {buttonCorrectSmall}
      {buttonIgnoreSmall}
    </>
  ) : undefined;

  return (
    <Container data-cy="qa-check-item">
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
        {!showButtonRow && !showDiff && buttonIgnoreBig}
      </StyledHeader>

      {showDiff && (
        <TextBlock actions={textActions}>
          <StyledDiffText>
            {renderDiff(
              text,
              issue.positionStart,
              issue.positionEnd,
              issue.replacement ?? ''
            )}
          </StyledDiffText>
        </TextBlock>
      )}

      {showButtonRow && (
        <StyledNormalActions>
          {issue.state === 'OPEN' && buttonCorrectBig}
          {buttonIgnoreBig}
        </StyledNormalActions>
      )}
    </Container>
  );
};
