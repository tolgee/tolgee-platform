import { alpha, styled, Tooltip } from '@mui/material';
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
  border-radius: 2px;
  -webkit-box-decoration-break: clone;
  box-decoration-break: clone;
  transition: background-color 0.1s ease-out;
  &:hover {
    background-color: ${({ theme }) => alpha(theme.palette.error.main, 0.12)};
    transition: background-color 0.1s ease-in;
  }
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

  const replacement = issue.replacement;
  const handleCorrect =
    replacement != null && canEditTranslation(translationId)
      ? () => {
          correctTranslation({
            translationId,
            translationText,
            issue: {
              positionStart: issue.positionStart,
              positionEnd: issue.positionEnd,
              replacement,
            },
          });
        }
      : undefined;

  const ignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/suppressions',
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
