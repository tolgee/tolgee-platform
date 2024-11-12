import { useRef, useState } from 'react';
import { Box, styled, IconButton, SxProps } from '@mui/material';

import { ArrowDropDown } from 'tg.component/CustomIcons';
import { components } from 'tg.service/apiSchema.generated';
import { TextField } from 'tg.component/common/TextField';
import { FakeInput } from 'tg.component/FakeInput';

import { TaskSearchSelectPopover } from './TaskSearchSelectPopover';
import { Task } from './types';
import React from 'react';
import { TaskLabel } from '../TaskLabel';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledClearButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

type Props = {
  value: Task | null;
  onChange?: (task: Task | null) => void;
  label?: React.ReactNode;
  sx?: SxProps;
  className?: string;
  project: SimpleProjectModel;
};

export const TaskSearchSelect: React.FC<Props> = ({
  value,
  onChange,
  label,
  sx,
  className,
  project,
}) => {
  const anchorEl = useRef<HTMLAnchorElement>(null);
  const [isOpen, setIsOpen] = useState(false);

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleClick = () => {
    setIsOpen(true);
  };

  const handleSelectOrganization = async (task: Task | null) => {
    onChange?.(task);
    setIsOpen(false);
  };

  return (
    <>
      <Box display="grid" {...{ sx, className }}>
        <TextField
          variant="outlined"
          value={value ? <TaskLabel task={value} /> : ''}
          data-cy="task-select"
          minHeight={false}
          label={label}
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
                <StyledClearButton
                  size="small"
                  onClick={handleClick}
                  tabIndex={-1}
                  sx={{ pointerEvents: 'none' }}
                >
                  <ArrowDropDown />
                </StyledClearButton>
              </Box>
            ),
          }}
        />

        <TaskSearchSelectPopover
          open={isOpen}
          onClose={handleClose}
          selected={value}
          onSelect={handleSelectOrganization}
          anchorEl={anchorEl.current!}
          project={project}
          sx={{
            width: (anchorEl.current?.offsetWidth ?? 100) + 40,
          }}
        />
      </Box>
    </>
  );
};
