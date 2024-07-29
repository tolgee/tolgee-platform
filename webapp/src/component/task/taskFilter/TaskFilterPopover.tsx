import React, { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import {
  Checkbox,
  ListItemText,
  ListSubheader,
  Menu,
  MenuItem,
  styled,
} from '@mui/material';
import { useDebouncedCallback } from 'use-debounce';

import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { SubfilterAssignees } from './SubfilterAssignees';
import { SubfilterLanguages } from './SubfilterLanguages';
import { User } from '../assigneeSelect/types';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];
type TaskType = components['schemas']['TaskModel']['type'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledListSubheader = styled(ListSubheader)`
  line-height: unset;
  padding-top: ${({ theme }) => theme.spacing(2)};
  padding-bottom: ${({ theme }) => theme.spacing(0.5)};
`;

export type TaskFilterType = {
  languages?: LanguageModel[];
  assignees?: User[];
  type?: TaskType[];
};

type Props = {
  value: TaskFilterType;
  onChange: (value: TaskFilterType) => void;
  onClose: () => void;
  open: boolean;
  anchorEl: HTMLElement;
  project: SimpleProjectModel;
};

export const TaskFilterPopover: React.FC<Props> = ({
  value: initialValue,
  onChange,
  onClose,
  open,
  anchorEl,
  project,
}) => {
  const [value, setValue] = useState(initialValue);
  const debouncedOnChange = useDebouncedCallback(onChange, 200);

  function handleChange(value: TaskFilterType) {
    setValue(value);
    debouncedOnChange(value);
  }

  const { t } = useTranslate();
  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 10000 },
  });

  const toggleType = (type: TaskType) => () => {
    if (value.type?.includes(type)) {
      handleChange({
        ...value,
        type: [],
      });
    } else {
      handleChange({
        ...value,
        type: [type],
      });
    }
  };

  return (
    <Menu
      open={open}
      onClose={onClose}
      anchorEl={anchorEl}
      slotProps={{
        paper: {
          sx: {
            minWidth: anchorEl.offsetWidth,
          },
        },
      }}
    >
      <SubfilterAssignees
        value={value.assignees ?? []}
        onChange={(assignees) => handleChange({ ...value, assignees })}
        project={project}
      />
      <SubfilterLanguages
        value={value.languages ?? []}
        onChange={(languages) => handleChange({ ...value, languages })}
        languages={languages.data?._embedded?.languages ?? []}
        project={project}
      />

      <StyledListSubheader disableSticky>
        {t('task_filter_type_label')}
      </StyledListSubheader>
      <MenuItem
        onClick={toggleType('TRANSLATE')}
        selected={Boolean(value.type?.includes('TRANSLATE'))}
      >
        <Checkbox
          checked={Boolean(value.type?.includes('TRANSLATE'))}
          size="small"
          edge="start"
          disableRipple
        />
        <ListItemText primary={t('task_filter_translate')} />
      </MenuItem>
      <MenuItem
        onClick={toggleType('REVIEW')}
        selected={Boolean(value.type?.includes('REVIEW'))}
      >
        <Checkbox
          checked={Boolean(value.type?.includes('REVIEW'))}
          size="small"
          edge="start"
          disableRipple
        />
        <ListItemText primary={t('task_filter_review')} />
      </MenuItem>
    </Menu>
  );
};
