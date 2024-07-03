import { useRef, useState } from 'react';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { XClose } from '@untitled-ui/icons-react';
import { Box, styled, IconButton, SxProps } from '@mui/material';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { TextField } from 'tg.component/common/TextField';
import { ProjectSearchSelectPopover } from './ProjectSearchSelectPopover';
import { Project } from './types';
import { FakeInput } from 'tg.component/FakeInput';

const StyledClearButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

type Props = {
  value: Project[];
  onChange?: (projects: Project[]) => void;
  label?: React.ReactNode;
  sx?: SxProps;
  className?: string;
};

export const ProjectSearchSelect: React.FC<Props> = ({
  value,
  onChange,
  label,
  sx,
  className,
}) => {
  const anchorEl = useRef<HTMLAnchorElement>(null);
  const [isOpen, setIsOpen] = useState(false);

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleClick = () => {
    setIsOpen(true);
  };

  const handleSelectOrganization = async (projects: Project[]) => {
    onChange?.(projects);
    setIsOpen(false);
  };

  const handleClear = () => {
    onChange?.([]);
  };

  return (
    <>
      <Box display="grid" {...{ sx, className }}>
        <TextField
          variant="outlined"
          value={value.map((u) => u.name).join(', ')}
          data-cy="project-select"
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
                {Boolean(value.length) && (
                  <StyledClearButton
                    size="small"
                    onClick={stopAndPrevent(handleClear)}
                    tabIndex={-1}
                  >
                    <XClose width={18} />
                  </StyledClearButton>
                )}
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

        <ProjectSearchSelectPopover
          open={isOpen}
          onClose={handleClose}
          selected={value}
          onSelect={handleSelectOrganization}
          anchorEl={anchorEl.current!}
        />
      </Box>
    </>
  );
};
