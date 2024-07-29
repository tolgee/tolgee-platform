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
import { stopAndPrevent, stopBubble } from 'tg.fixtures/eventHandler';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledInputButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

type Props = {
  value: TaskFilterType;
  onChange: (value: TaskFilterType) => void;
  project: SimpleProjectModel;
  sx?: SxProps;
  className?: string;
};

function getFilterValue(value: TaskFilterType) {
  if (filterEmpty(value)) {
    return null;
  }

  return (
    <Box display="flex" gap="6px" alignItems="center">
      {value.assignees?.map((user) => (
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
      {value.languages?.map((language) => (
        <FlagImage
          key={language.id}
          flagEmoji={language.flagEmoji!}
          height={20}
        />
      ))}
      {value.type?.map((type) => (
        <TaskTypeChip key={type} type={type} />
      ))}
    </Box>
  );
}

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
        />
      )}
    </>
  );
};
