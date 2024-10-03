import { Box, IconButton, styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useNumberFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/apiSchema.generated';
import { BatchProgress } from 'tg.views/projects/translations/BatchOperations/OperationsSummary/BatchProgress';
import { TaskState } from './TaskState';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import React from 'react';
import { UserName } from 'tg.component/common/UserName';
import { File06 } from '@untitled-ui/icons-react';
import { useTaskReport } from './utils';

type TaskModel = components['schemas']['TaskModel'];
type TaskPerUserReportModel = components['schemas']['TaskPerUserReportModel'];

const StyledScope = styled(Box)`
  display: grid;
  position: relative;
  background: ${({ theme }) => theme.palette.tokens.background.selected};
  padding: 24px;
  border-radius: 8px;
  grid-template-columns: 3fr 1fr 1fr 1fr auto;
  gap: 6px;
`;

type Props = {
  task: TaskModel;
  perUserData: TaskPerUserReportModel[] | undefined;
  projectId: number;
};

export const TaskScope = ({ task, perUserData, projectId }: Props) => {
  const formatNumber = useNumberFormatter();
  const { t } = useTranslate();
  const { downloadReport } = useTaskReport();

  return (
    <StyledScope>
      <Box display="flex" alignItems="center" gap={1}>
        <TaskState state={task.state} />
        <Box sx={{ width: 100 }}>
          <BatchProgress progress={task.doneItems} max={task.totalItems} />
        </Box>
        {Boolean(task.totalItems) && (
          <Box sx={{ fontSize: 12 }}>
            {formatNumber((task.doneItems / task.totalItems) * 100, {
              maximumFractionDigits: 0,
            })}
            %
          </Box>
        )}
      </Box>
      <Box>{t('task_scope_keys_label')}</Box>
      <Box>{t('task_scope_words_label')}</Box>
      <Box>{t('task_scope_characters_label')}</Box>
      <Tooltip title={t('task_detail_summarize_tooltip')} disableInteractive>
        <IconButton
          data-cy="task-detail-download-report"
          onClick={() => downloadReport(projectId, task)}
          sx={{ margin: -1, position: 'relative', left: -8 }}
        >
          <File06 />
        </IconButton>
      </Tooltip>

      <Box sx={{ gridColumn: 1 }}>{t('task_scope_total_to_translate')}</Box>
      <Box data-cy="task-detail-keys">{formatNumber(task.totalItems)}</Box>
      <Box data-cy="task-detail-words">{formatNumber(task.baseWordCount)}</Box>
      <Box data-cy="task-detail-characters">
        {formatNumber(task.baseCharacterCount)}
      </Box>

      <Box sx={{ gridColumn: 1, height: '4px' }} />

      {perUserData?.map((item, i) => (
        <React.Fragment key={i}>
          <Box
            sx={{
              gridColumn: 1,
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <AvatarImg
              owner={{
                avatar: item.user.avatar,
                id: item.user.id,
                name: item.user.name,
                type: 'USER',
              }}
              size={24}
            />
            <Box>
              <UserName {...item.user} />
            </Box>
          </Box>
          <Box
            data-cy="task-detail-user-keys"
            data-cy-user={item.user.name}
            sx={{ alignSelf: 'center' }}
          >
            {formatNumber(item.doneItems)}
          </Box>
          <Box
            data-cy="task-detail-user-words"
            data-cy-user={item.user.name}
            sx={{ alignSelf: 'center' }}
          >
            {formatNumber(item.baseWordCount)}
          </Box>
          <Box
            data-cy="task-detail-user-characters"
            data-cy-user={item.user.name}
            sx={{ alignSelf: 'center' }}
          >
            {formatNumber(item.baseCharacterCount)}
          </Box>
        </React.Fragment>
      ))}
    </StyledScope>
  );
};
