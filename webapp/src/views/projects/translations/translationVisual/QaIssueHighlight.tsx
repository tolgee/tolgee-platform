import { styled, Tooltip } from '@mui/material';
import { TooltipCard } from 'tg.component/common/TooltipCard';
import { QaCheckItem } from 'tg.ee';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useTranslationsActions } from '../context/TranslationsContext';

type QaIssueModel = components['schemas']['QaIssueModel'];

const StyledHighlight = styled('span')`
  text-decoration: underline;
  text-decoration-color: ${({ theme }) => theme.palette.error.main};
`;

type Props = {
  text: string;
  translationText: string;
  issue: QaIssueModel;
  translationId: number;
};

export const QaIssueHighlight = ({
  text,
  translationText,
  issue,
  translationId,
}: Props) => {
  const project = useProject();
  const { correctTranslation, canEditTranslation } = useTranslationsActions();

  const handleCorrect =
    issue.replacement != null && canEditTranslation(translationId)
      ? () => {
          correctTranslation({
            translationId,
            translationText,
            issue: {
              positionStart: issue.positionStart,
              positionEnd: issue.positionEnd,
              replacement: issue.replacement!,
            },
          });
        }
      : undefined;

  const ignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/ignore',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/translations',
  });

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
      <StyledHighlight data-cy="qa-issue-highlight">{text}</StyledHighlight>
    </Tooltip>
  );
};
