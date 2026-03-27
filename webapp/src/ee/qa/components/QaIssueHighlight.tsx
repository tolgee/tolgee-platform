import { alpha, styled, Tooltip } from '@mui/material';
import { TooltipCard } from 'tg.component/common/TooltipCard';
import { QaCheckItem } from 'tg.ee';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useReportEvent } from 'tg.hooks/useReportEvent';
import { useTranslationsActions } from 'tg.views/projects/translations/context/TranslationsContext';
import { QaIssueHighlightProps } from '../../../eeSetup/EeModuleType';

type QaIssueModel = components['schemas']['QaIssueModel'];

const StyledHighlight = styled('span')`
  text-decoration: underline;
  text-decoration-color: ${({ theme }) => theme.palette.error.main};
  border-radius: 2px;
  -webkit-box-decoration-break: clone;
  box-decoration-break: clone;
  transition: background-color 0.1s ease-out;
  &:hover {
    background-color: ${({ theme }) => alpha(theme.palette.error.main, 0.12)};
    transition: background-color 0.1s ease-in;
  }
`;

const StyledMarker = styled('span')`
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: ${({ theme }) => theme.palette.error.main};
  vertical-align: text-bottom;
  border-radius: 1px;
  cursor: pointer;
`;

export const QaIssueHighlight = ({
  text,
  translationText,
  issue,
  translationId,
}: QaIssueHighlightProps) => {
  const project = useProject();
  const { correctTranslation, canEditTranslation } = useTranslationsActions();
  const reportEvent = useReportEvent();

  const replacement = issue.replacement;
  const positionStart = issue.positionStart;
  const positionEnd = issue.positionEnd;

  const isCorrectable =
    replacement != null &&
    positionStart != null &&
    positionEnd != null &&
    canEditTranslation(translationId);

  const handleCorrect = isCorrectable
    ? () => {
        correctTranslation({
          translationId,
          translationText,
          issue: {
            positionStart,
            positionEnd,
            replacement,
          },
        });
        reportEvent('QA_ISSUE_CORRECTED_HIGHLIGHT', {
          checkType: issue.type,
        });
      }
    : undefined;

  const ignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/suppressions',
    method: 'post',
  });

  const hasVisibleContent = text.length > 0 && /[^\r\n]/.test(text);

  const handleIgnore = () => {
    ignoreMutation.mutate({
      path: {
        projectId: project.id,
        translationId,
      },
      content: {
        'application/json': issue,
      },
    });
  };

  return (
    <Tooltip
      placement="bottom-start"
      enterDelay={200}
      leaveDelay={200}
      components={{ Tooltip: TooltipCard }}
      title={
        <div onClick={stopBubble()}>
          <QaCheckItem
            issue={issue}
            text={translationText}
            onCorrect={handleCorrect}
            onIgnore={handleIgnore}
          />
        </div>
      }
    >
      {hasVisibleContent ? (
        <StyledHighlight data-cy="qa-issue-highlight">{text}</StyledHighlight>
      ) : (
        <span>
          {text}
          <StyledMarker data-cy="qa-issue-marker" />
        </span>
      )}
    </Tooltip>
  );
};
