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
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';

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
  filterLanguages: number[];
  onFilterLanguagesChange: (value: number[]) => void;
};

export const TasksHeader = ({
  sx,
  className,
  onSearchChange,
  showClosed,
  onShowClosedChange,
  filterLanguages,
  onFilterLanguagesChange,
}: Props) => {
  const [localSearch, setLocalSearch] = useState('');
  const onDebouncedSearchChange = useDebounceCallback(onSearchChange, 500);
  const project = useProject();
  const { t } = useTranslate();
  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: {
      page: 0,
      size: 1000,
      sort: ['tag'],
    },
  });

  const languages = languagesLoadable.data?._embedded?.languages ?? [];

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
        <FormControlLabel
          checked={showClosed}
          onChange={() => onShowClosedChange(!showClosed)}
          control={<Checkbox size="small" />}
          label={t('tasks_show_closed_label')}
        />
      </Box>
      <LanguagesSelect
        onChange={(tags) =>
          onFilterLanguagesChange(
            languages.filter((l) => tags.includes(l.tag)).map((l) => l.id)
          )
        }
        value={languages
          .filter((l) => filterLanguages.includes(l.id))
          .map((l) => l.tag)}
        languages={languages || []}
        enableEmpty
        context="tasks"
        placeholder={t('tasks_filter_by_language')}
      />
    </StyledContainer>
  );
};
