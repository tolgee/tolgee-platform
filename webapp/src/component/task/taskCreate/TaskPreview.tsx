import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { stringHash } from 'tg.fixtures/stringHash';
import { Box, Skeleton, styled, Tooltip } from '@mui/material';
import { FlagImage } from 'tg.component/languages/FlagImage';
import { useTranslate } from '@tolgee/react';
import { useNumberFormatter } from 'tg.hooks/useLocale';
import { Warning } from '@mui/icons-material';
import { AssigneeSearchSelect } from '../assigneeSelect/AssigneeSearchSelect';
import { User } from '../assigneeSelect/types';

type TaskType = components['schemas']['TaskModel']['type'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: grid;
  padding: 16px 20px;
  grid-template-columns: 1fr 3fr 2fr;
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.tokens.background.selected};
`;

const StyledContent = styled('div')`
  display: flex;
  align-items: center;
  padding: 8px 12px;
  flex-shrink: 0;
  gap: 8px;
`;

const StyledMetric = styled('div')`
  font-size: 15px;
  color: ${({ theme }) => theme.palette.tokens.text.secondary};
`;

const StyledSmallCaption = styled('div')`
  font-size: 12px;
  position: relative;
  margin-bottom: -3px;
`;

type Props = {
  type: TaskType;
  language: LanguageModel;
  keys: number[];
  assigness: User[];
  onUpdateAssignees: (users: User[]) => void;
};

export const TaskPreview = ({
  type,
  language,
  keys,
  assigness,
  onUpdateAssignees,
}: Props) => {
  const { t } = useTranslate();
  const formatNumber = useNumberFormatter();

  const project = useProject();
  const content = { keys, type, language: language.id };
  const statsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/calculate-scope',
    method: 'post',
    path: { projectId: project.id },
    content: { 'application/json': content },
    // @ts-ignore add dependencies to url, so react query works correctly
    query: { hash: stringHash(JSON.stringify(content)) },
  });

  return (
    <StyledContainer>
      <StyledContent>
        <FlagImage flagEmoji={language.flagEmoji!} height={20} />
        <Box sx={{ fontWeight: language.base ? 'bold' : 'normal' }}>
          {language.name}
        </Box>
      </StyledContent>

      <Box display="flex" gap="60px" justifyContent="center">
        <Box display="grid" alignContent="center">
          <StyledMetric>{t('create_task_preview_keys')}</StyledMetric>
          <StyledMetric>
            {statsLoadable.data ? (
              <Box display="flex" gap={0.5} alignItems="center">
                {formatNumber(statsLoadable.data.keyCount)}
                {statsLoadable.data.keyCount !== keys.length && (
                  <Tooltip title={t('create_task_preview_missing_keys_hint')}>
                    <Warning fontSize="inherit" color="warning" />
                  </Tooltip>
                )}
              </Box>
            ) : (
              <Skeleton />
            )}
          </StyledMetric>
        </Box>
        <Box display="grid" alignContent="center">
          <StyledMetric>{t('create_task_preview_words')}</StyledMetric>
          <StyledMetric>
            {statsLoadable.data ? (
              formatNumber(statsLoadable.data.wordCount)
            ) : (
              <Skeleton />
            )}
          </StyledMetric>
        </Box>
      </Box>
      <AssigneeSearchSelect
        value={assigness}
        project={project}
        onChange={onUpdateAssignees}
        sx={{
          alignSelf: 'center',
        }}
        label={
          <StyledSmallCaption>
            {t('create_task_preview_assignee')}
          </StyledSmallCaption>
        }
      />
    </StyledContainer>
  );
};
