import { useState } from 'react';
import {
  Box,
  Button,
  ButtonGroup,
  Checkbox,
  FormControlLabel,
  InputAdornment,
  styled,
  SxProps,
  Tooltip,
} from '@mui/material';
import { useDebounceCallback } from 'usehooks-ts';
import {
  BarChartSquare01,
  Plus,
  Rows03,
  SearchSm,
  ShoppingCart01,
} from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { TextField } from 'tg.component/common/TextField';
import { LabelHint } from 'tg.component/common/LabelHint';
import { components } from 'tg.service/apiSchema.generated';

import { TaskFilterType } from '../taskFilter/TaskFilterPopover';
import { TaskFilter } from '../taskFilter/TaskFilter';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledContainer = styled(Box)`
  display: flex;
  gap: 16px;
  justify-content: space-between;
`;

const StyledToggleButton = styled(Button)`
  padding: 4px 8px;
`;

export type TaskView = 'LIST' | 'BOARD';

type Props = {
  sx?: SxProps;
  className?: string;
  onSearchChange: (value: string) => void;
  showAll: boolean;
  onShowAllChange: (value: boolean) => void;
  filter: TaskFilterType;
  onFilterChange: (value: TaskFilterType) => void;
  onAddTask?: () => void;
  onOrderTranslation?: () => void;
  view: TaskView;
  onViewChange: (view: TaskView) => void;
  project?: SimpleProjectModel;
};

export const TasksHeaderBig = ({
  sx,
  className,
  onSearchChange,
  showAll,
  onShowAllChange,
  filter,
  onFilterChange,
  onAddTask,
  onOrderTranslation,
  view,
  onViewChange,
  project,
}: Props) => {
  const [localSearch, setLocalSearch] = useState('');
  const onDebouncedSearchChange = useDebounceCallback(onSearchChange, 500);
  const { t } = useTranslate();

  return (
    <StyledContainer {...{ sx, className }}>
      <Box sx={{ display: 'flex', gap: '16px' }}>
        <TextField
          minHeight={false}
          value={localSearch}
          onChange={(e) => {
            setLocalSearch(e.target.value);
            onDebouncedSearchChange(e.target.value);
          }}
          placeholder={t('tasks_search_placeholder')}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchSm width={20} height={20} />
              </InputAdornment>
            ),
          }}
        />
        <TaskFilter
          value={filter}
          onChange={onFilterChange}
          sx={{ minWidth: '230px', maxWidth: '230px' }}
          project={project}
        />
        <FormControlLabel
          checked={showAll}
          onChange={() => onShowAllChange(!showAll)}
          control={<Checkbox size="small" />}
          data-cy="tasks-header-show-all"
          sx={{ whiteSpace: 'nowrap' }}
          label={
            <Box display="flex">
              {t('tasks_show_all_label')}
              <LabelHint title={t('tasks_show_all_label_tooltip')}>
                <span />
              </LabelHint>
            </Box>
          }
        />
      </Box>
      <Box sx={{ display: 'flex', gap: '12px' }}>
        <ButtonGroup>
          <StyledToggleButton
            color={view === 'LIST' ? 'primary' : 'default'}
            onClick={() => onViewChange('LIST')}
            data-cy="tasks-view-list-button"
          >
            <Rows03 />
          </StyledToggleButton>
          <StyledToggleButton
            color={view === 'BOARD' ? 'primary' : 'default'}
            onClick={() => onViewChange('BOARD')}
            data-cy="tasks-view-board-button"
          >
            <BarChartSquare01 style={{ rotate: '180deg' }} />
          </StyledToggleButton>
        </ButtonGroup>

        {onOrderTranslation && (
          <Tooltip title={t('tasks_order_translation_tooltip')}>
            <Button
              variant="outlined"
              color="primary"
              startIcon={<ShoppingCart01 width={19} height={19} />}
              onClick={onOrderTranslation}
              data-cy="tasks-header-order-translation"
            >
              {t('tasks_order_translation')}
            </Button>
          </Tooltip>
        )}

        {onAddTask && (
          <Button
            variant="contained"
            color="primary"
            startIcon={<Plus width={19} height={19} />}
            onClick={onAddTask}
            data-cy="tasks-header-add-task"
          >
            {t('tasks_add')}
          </Button>
        )}
      </Box>
    </StyledContainer>
  );
};
