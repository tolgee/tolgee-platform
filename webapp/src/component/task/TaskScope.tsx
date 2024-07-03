import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useNumberFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/apiSchema.generated';
import { BatchProgress } from 'tg.views/projects/translations/BatchOperations/OperationsSummary/BatchProgress';
import { TaskState } from './TaskState';

type TaskModel = components['schemas']['TaskModel'];

const StyledScope = styled(Box)`
  display: grid;
  background: ${({ theme }) => theme.palette.tokens.background.selected};
  padding: 24px;
  border-radius: 8px;
  grid-template-columns: 3fr 1fr 1fr 1fr;
`;

type Props = {
  task: TaskModel;
};

export const TaskScope = ({ task }: Props) => {
  const formatNumber = useNumberFormatter();
  const { t } = useTranslate();

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
      <Box>{t('task_scope_words_label')}</Box>
      <Box>{t('task_scope_keys_label')}</Box>

      <Box sx={{ gridColumn: 1 }}>{t('task_scope_total_to_translate')}</Box>
      <Box>{formatNumber(task.baseWordCount)}</Box>
      <Box>{formatNumber(task.totalItems)}</Box>

      {/* <Box sx={{ gridColumn: '1 / -1', height: 16 }} /> */}
    </StyledScope>
  );
};
