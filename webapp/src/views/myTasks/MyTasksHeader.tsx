import { useState } from 'react';
import {
  Box,
  Checkbox,
  FormControlLabel,
  InputAdornment,
  styled,
  SxProps,
} from '@mui/material';
import { useDebounceCallback } from 'usehooks-ts';

import { TextField } from 'tg.component/common/TextField';
import { Search } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';
import { TaskFilterType } from 'tg.component/task/taskFilter/TaskFilterPopover';
import { TaskFilter } from 'tg.component/task/taskFilter/TaskFilter';

const StyledContainer = styled(Box)`
  display: flex;
  gap: 16px;
  justify-content: space-between;
`;

type Props = {
  sx?: SxProps;
  className?: string;
  onSearchChange: (value: string) => void;
  showClosed: boolean;
  onShowClosedChange: (value: boolean) => void;
  filter: TaskFilterType;
  onFilterChange: (value: TaskFilterType) => void;
};

export const MyTasksHeader = ({
  sx,
  className,
  onSearchChange,
  showClosed,
  onShowClosedChange,
  filter,
  onFilterChange,
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
                <Search fontSize="small" />
              </InputAdornment>
            ),
          }}
        />
        <TaskFilter
          value={filter}
          onChange={onFilterChange}
          sx={{ minWidth: '230px', maxWidth: '230px' }}
        />
        <FormControlLabel
          checked={showClosed}
          onChange={() => onShowClosedChange(!showClosed)}
          control={<Checkbox size="small" />}
          label={t('tasks_show_closed_label')}
        />
      </Box>
    </StyledContainer>
  );
};
