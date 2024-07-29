import { ArrowDropDown, Close } from '@mui/icons-material';
import { Box, IconButton, styled, SxProps } from '@mui/material';
import { useRef, useState } from 'react';
import { TextField } from 'tg.component/common/TextField';
import { FakeInput } from 'tg.component/FakeInput';
import { TaskFilterPopover, TaskFilterType } from './TaskFilterPopover';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { FlagImage } from 'tg.component/languages/FlagImage';
import { TaskTypeChip } from '../TaskTypeChip';
import { filterEmpty } from './taskFilterUtils';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledInputButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

const StyledContent = styled(Box)`
  display: flex;
  gap: 6px;
  align-items: center;
  & > * {
    flex-shrink: 0;
  }
`;

type Props = {
  value: TaskFilterType;
  onChange: (value: TaskFilterType) => void;
  project: SimpleProjectModel;
  sx?: SxProps;
  className?: string;
};

export const TaskFilter = ({
  value,
  onChange,
  project,
  sx,
  className,
}: Props) => {
  const anchorEl = useRef(null);
  const [open, setOpen] = useState(false);
  const { t } = useTranslate();

  const usersLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/possible-assignees',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000, filterId: value.assignees },
    options: {
      enabled: Boolean(value.assignees?.length),
    },
  });

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 10000 },
  });

  const languages = languagesLoadable.data?._embedded?.languages ?? [];

  function getFilterValue(value: TaskFilterType) {
    if (filterEmpty(value)) {
      return null;
    }

    return (
      <StyledContent>
        {usersLoadable.data?._embedded?.users
          ?.filter((u) => value.assignees?.includes(u.id))
          ?.map((user) => (
            <AvatarImg
              key={user.id}
              owner={{
                name: user.name,
                avatar: user.avatar,
                type: 'USER',
                id: user.id,
              }}
              size={24}
            />
          ))}
        {languages
          ?.filter((l) => value.languages?.includes(l.id))
          .map((language) => (
            <FlagImage
              key={language.id}
              flagEmoji={language.flagEmoji!}
              height={20}
            />
          ))}
        {value.types?.map((type) => (
          <TaskTypeChip key={type} type={type} />
        ))}
      </StyledContent>
    );
  }

  function handleClick() {
    setOpen(true);
  }

  return (
    <>
      <TextField
        variant="outlined"
        value={getFilterValue(value)}
        data-cy="assignee-select"
        minHeight={false}
        placeholder={t('task_filter_placeholder')}
        InputProps={{
          onClick: handleClick,
          ref: anchorEl,
          fullWidth: true,
          sx: {
            cursor: 'pointer',
          },
          readOnly: true,
          inputComponent: FakeInput,
          margin: 'dense',
          endAdornment: (
            <Box sx={{ display: 'flex', marginRight: -0.5 }}>
              {!filterEmpty(value) && (
                <StyledInputButton
                  size="small"
                  onClick={stopBubble(() => onChange({}))}
                  tabIndex={-1}
                >
                  <Close fontSize="small" />
                </StyledInputButton>
              )}
              <StyledInputButton
                size="small"
                onClick={handleClick}
                tabIndex={-1}
                sx={{ pointerEvents: 'none' }}
              >
                <ArrowDropDown />
              </StyledInputButton>
            </Box>
          ),
        }}
        {...{ sx, className }}
      />
      {open && (
        <TaskFilterPopover
          open={open}
          onClose={() => setOpen(false)}
          value={value}
          onChange={onChange}
          anchorEl={anchorEl.current!}
          project={project}
          languages={languages}
        />
      )}
    </>
  );
};
