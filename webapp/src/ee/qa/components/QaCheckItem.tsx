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
import { cropDiffContext } from 'tg.fixtures/cropDiffContext';

const StyledItem = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(1)};
  margin: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledDisabledItem = styled(StyledItem)`
  opacity: 0.5;
`;

const StyledHeader = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
`;

const StyledTypeLine = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.75)};
  min-width: 0;
`;

const StyledVariantBadge = styled('span')`
  display: inline-flex;
  align-items: center;
  height: ${({ theme }) => theme.spacing(2.5)};
  padding: 0 ${({ theme }) => theme.spacing(1)};
  border-radius: ${({ theme }) => theme.spacing(1.5)};
  font-size: 11px;
  line-height: 1;
  background: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[100]
      : theme.palette.emphasis[200]};
  color: ${({ theme }) => theme.palette.text.primary};
  flex-shrink: 0;
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

const StyledIconButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-0.5)}
    ${({ theme }) => theme.spacing(-0.2)};
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

const StyledInsertionMarker = styled('span')`
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: ${({ theme }) => theme.palette.primary.main};
  vertical-align: text-bottom;
  border-radius: 1px;
  margin: 0 1px;
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
  replacement: string,
  locale?: string
) {
  const cropped = cropDiffContext(
    text,
    positionStart,
    positionEnd,
    replacement,
    locale
  );
  const isReplacementSpacesOnly = /^\s+$/.test(cropped.replacement);
  return (
    <>
      {cropped.beforeEllipsis && <StyledUnchanged>…</StyledUnchanged>}
      {cropped.before && (
        <StyledUnchanged>
          <bdi>{cropped.before}</bdi>
        </StyledUnchanged>
      )}
      {cropped.removed && (
        <StyledRemoved>
          <bdi>{cropped.removed}</bdi>
        </StyledRemoved>
      )}
      {cropped.isInsertion && cropped.replacement && <StyledInsertionMarker />}
      {cropped.replacement &&
        (isReplacementSpacesOnly ? (
          <StyledAddedSpaces>{cropped.replacement}</StyledAddedSpaces>
        ) : (
          <StyledAdded>
            <bdi>{cropped.replacement}</bdi>
          </StyledAdded>
        ))}
      {cropped.after && (
        <StyledUnchanged>
          <bdi>{cropped.after}</bdi>
        </StyledUnchanged>
      )}
      {cropped.afterEllipsis && <StyledUnchanged>…</StyledUnchanged>}
    </>
  );
}

type Props = {
  issue: QaPreviewIssue;
  index?: number;
  text: string;
  locale?: string;
  slim?: boolean;
  disabled?: boolean;
  onCorrect?: () => void;
  onIgnore?: () => void;
};

export const QaCheckItem = ({
  issue,
  index,
  text,
  locale,
  slim = false,
  disabled = false,
  onCorrect,
  onIgnore,
}: Props) => {
  const { t } = useTranslate();
  const typeLabel = useQaCheckTypeLabel(issue.type);
  const messageText = useQaIssueMessage(issue.message, issue.params);
  const hasReplacement =
    issue.replacement != null &&
    issue.positionStart != null &&
    issue.positionEnd != null;

  const showDiff = !disabled && hasReplacement && issue.state === 'OPEN';

  const Container =
    disabled || issue.state === 'IGNORED' ? StyledDisabledItem : StyledItem;

  const buttonCorrectSmall = onCorrect && (
    <Tooltip title={t('qa_check_action_correct')}>
      <StyledIconButton
        size="small"
        color="primary"
        onClick={onCorrect}
        data-cy="qa-action-correct"
      >
        <Check width={20} height={20} />
      </StyledIconButton>
    </Tooltip>
  );

  const buttonIgnoreSmall = onIgnore && (
    <Tooltip title={t('qa_check_action_ignore')}>
      <StyledIconButton
        size="small"
        onClick={onIgnore}
        data-cy="qa-action-ignore"
        data-cy-state={issue.state}
      >
        <XClose width={20} height={20} />
      </StyledIconButton>
    </Tooltip>
  );

  const buttonCorrectBig = onCorrect && (
    <Button
      variant="outlined"
      color="primary"
      size="small"
      onClick={onCorrect}
      data-cy="qa-action-correct"
    >
      <T keyName="qa_check_action_correct" />
    </Button>
  );

  const buttonIgnoreBig = onIgnore && (
    <Button
      variant="text"
      color="inherit"
      size="small"
      onClick={onIgnore}
      data-cy="qa-action-ignore"
      data-cy-state={issue.state}
    >
      {issue.state === 'IGNORED' ? (
        <T keyName="qa_check_action_unignore" />
      ) : (
        <T keyName="qa_check_action_ignore" />
      )}
    </Button>
  );

  const textActions =
    slim && !disabled ? (
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
          <StyledTypeLine>
            <Typography variant="body2">{typeLabel}</Typography>
            {issue.pluralVariant && (
              <StyledVariantBadge data-cy="qa-check-item-variant-badge">
                {issue.pluralVariant}
              </StyledVariantBadge>
            )}
          </StyledTypeLine>
          <StyledMessage>{messageText}</StyledMessage>
        </StyledContent>
        {slim && !showDiff && !disabled && buttonIgnoreBig}
      </StyledHeader>

      {showDiff && (
        <TextBlock actions={textActions}>
          <StyledDiffText>
            {renderDiff(
              text,
              issue.positionStart ?? 0,
              issue.positionEnd ?? 0,
              issue.replacement ?? '',
              locale
            )}
          </StyledDiffText>
        </TextBlock>
      )}

      {!slim && !disabled && (
        <StyledNormalActions>
          {issue.state === 'OPEN' && buttonCorrectBig}
          {buttonIgnoreBig}
        </StyledNormalActions>
      )}
    </Container>
  );
};
