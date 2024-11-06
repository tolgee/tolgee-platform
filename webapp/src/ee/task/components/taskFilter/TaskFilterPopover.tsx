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
import { SubfilterAssignees } from './SubfilterAssignees';
import { SubfilterLanguages } from './SubfilterLanguages';
import { SubfilterProjects } from './SubfilterProjects';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];
type TaskType = components['schemas']['TaskModel']['type'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledListSubheader = styled(ListSubheader)`
  line-height: unset;
  padding-top: ${({ theme }) => theme.spacing(2)};
  padding-bottom: ${({ theme }) => theme.spacing(0.5)};
`;

export type TaskFilterType = {
  languages?: number[];
  assignees?: number[];
  projects?: number[];
  types?: TaskType[];
  doneMinClosedAt?: number;
};

type Props = {
  value: TaskFilterType;
  onChange: (value: TaskFilterType) => void;
  onClose: () => void;
  open: boolean;
  anchorEl: HTMLElement;
  project?: SimpleProjectModel;
  languages: LanguageModel[];
};

export const TaskFilterPopover: React.FC<Props> = ({
  value: initialValue,
  onChange,
  onClose,
  open,
  anchorEl,
  project,
  languages,
}) => {
  const [value, setValue] = useState(initialValue);
  const debouncedOnChange = useDebouncedCallback(onChange, 200);

  function handleChange(value: TaskFilterType) {
    setValue(value);
    debouncedOnChange(value);
  }

  const { t } = useTranslate();

  const toggleType = (type: TaskType) => () => {
    if (value.types?.includes(type)) {
      handleChange({
        ...value,
        types: [],
      });
    } else {
      handleChange({
        ...value,
        types: [type],
      });
    }
  };

  return (
    <Menu
      data-cy="tasks-filter-menu"
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
      {project && (
        <SubfilterAssignees
          value={value.assignees ?? []}
          onChange={(assignees) => handleChange({ ...value, assignees })}
          projectId={project.id}
        />
      )}

      {project && (
        <SubfilterLanguages
          value={value.languages ?? []}
          onChange={(languages) => handleChange({ ...value, languages })}
          languages={languages ?? []}
        />
      )}

      {!project && (
        <SubfilterProjects
          value={value.projects ?? []}
          onChange={(projects) => handleChange({ ...value, projects })}
        />
      )}

      <StyledListSubheader disableSticky>
        {t('task_filter_type_label')}
      </StyledListSubheader>
      <MenuItem
        onClick={toggleType('TRANSLATE')}
        selected={Boolean(value.types?.includes('TRANSLATE'))}
      >
        <Checkbox
          checked={Boolean(value.types?.includes('TRANSLATE'))}
          size="small"
          edge="start"
          disableRipple
        />
        <ListItemText primary={t('task_filter_translate')} />
      </MenuItem>
      <MenuItem
        onClick={toggleType('REVIEW')}
        selected={Boolean(value.types?.includes('REVIEW'))}
      >
        <Checkbox
          checked={Boolean(value.types?.includes('REVIEW'))}
          size="small"
          edge="start"
          disableRipple
        />
        <ListItemText primary={t('task_filter_review')} />
      </MenuItem>
    </Menu>
  );
};
