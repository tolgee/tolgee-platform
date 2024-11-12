import { useRef, useState } from 'react';
import {
  Badge,
  Box,
  Checkbox,
  FormControlLabel,
  IconButton,
  styled,
  SxProps,
} from '@mui/material';
import { useDebounceCallback } from 'usehooks-ts';
import { FilterLines, Plus, SearchSm, XClose } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { LabelHint } from 'tg.component/common/LabelHint';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { HeaderSearchField } from 'tg.component/layout/HeaderSearchField';
import { components } from 'tg.service/apiSchema.generated';
import {
  TaskFilterPopover,
  TaskFilterType,
} from '../taskFilter/TaskFilterPopover';
import { filterEmpty } from '../taskFilter/taskFilterUtils';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledContainer = styled(Box)`
  display: flex;
  gap: 16px;
  justify-content: space-between;
`;

const StyledIconButton = styled(IconButton)`
  width: 38px;
  height: 38px;
`;

const StyledSearchSpaced = styled('div')`
  display: grid;
  flex-grow: 1;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  position: relative;
`;

const StyledSearch = styled(HeaderSearchField)`
  min-width: 200px;
`;

export type TaskView = 'LIST' | 'BOARD';

type Props = {
  sx?: SxProps;
  className?: string;
  onSearchChange: (value: string) => void;
  showClosed: boolean;
  onShowClosedChange: (value: boolean) => void;
  filter: TaskFilterType;
  onFilterChange: (value: TaskFilterType) => void;
  onAddTask?: () => void;
  view: TaskView;
  onViewChange: (view: TaskView) => void;
  project?: SimpleProjectModel;
};

export const TasksHeaderCompact = ({
  sx,
  className,
  onSearchChange,
  showClosed,
  onShowClosedChange,
  filter,
  onFilterChange,
  onAddTask,
  project,
}: Props) => {
  const [localSearch, setLocalSearch] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const onDebouncedSearchChange = useDebounceCallback(onSearchChange, 500);
  const { t } = useTranslate();
  const filtersAnchorEl = useRef(null);
  const [filtersOpen, setFiltersOpen] = useState(false);

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project?.id ?? 0 },
    query: { size: 10000 },
    options: {
      enabled: Boolean(project),
      keepPreviousData: true,
    },
  });

  const languages = languagesLoadable.data?._embedded?.languages ?? [];

  return (
    <StyledContainer {...{ sx, className }}>
      {searchOpen ? (
        <StyledSearchSpaced>
          <StyledSearch
            value={localSearch}
            onSearchChange={(value) => {
              setLocalSearch(value);
              onDebouncedSearchChange(value);
            }}
            label={null}
            variant="outlined"
            placeholder={t('standard_search_label')}
            style={{
              height: 35,
              maxWidth: 'unset',
              width: '100%',
            }}
          />
          <StyledIconButton size="small" onClick={() => setSearchOpen(false)}>
            <XClose />
          </StyledIconButton>
        </StyledSearchSpaced>
      ) : (
        <>
          <Box sx={{ display: 'flex', gap: '8px' }}>
            <Badge
              color="primary"
              badgeContent={localSearch.length}
              variant="dot"
            >
              <StyledIconButton
                size="small"
                onClick={() => setSearchOpen(true)}
              >
                <SearchSm />
              </StyledIconButton>
            </Badge>
            <Badge
              color="primary"
              badgeContent={Number(!filterEmpty(filter))}
              variant="dot"
            >
              <StyledIconButton
                size="small"
                onClick={(e) => setFiltersOpen(true)}
                ref={filtersAnchorEl}
              >
                <FilterLines />
              </StyledIconButton>
            </Badge>
            {filtersOpen && (
              <TaskFilterPopover
                open={true}
                onClose={() => setFiltersOpen(false)}
                value={filter}
                onChange={onFilterChange}
                anchorEl={filtersAnchorEl.current!}
                project={project}
                languages={languages}
              />
            )}
            <FormControlLabel
              checked={showClosed}
              onChange={() => onShowClosedChange(!showClosed)}
              control={<Checkbox size="small" />}
              sx={{ pl: 1 }}
              label={
                <Box display="flex">
                  {t('tasks_show_closed_label')}
                  <LabelHint title={t('tasks_show_closed_label_tooltip')}>
                    <span />
                  </LabelHint>
                </Box>
              }
            />
          </Box>
          <Box sx={{ display: 'flex', gap: '16px' }}>
            {onAddTask && (
              <IconButton color="primary" onClick={onAddTask}>
                <Plus />
              </IconButton>
            )}
          </Box>
        </>
      )}
    </StyledContainer>
  );
};
