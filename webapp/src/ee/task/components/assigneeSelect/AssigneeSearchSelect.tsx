import { useRef, useState } from 'react';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { XClose } from '@untitled-ui/icons-react';
import { Box, styled, IconButton, SxProps, useTheme } from '@mui/material';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { TextField } from 'tg.component/common/TextField';
import {
  AssigneeFilters,
  AssigneeSearchSelectPopover,
} from './AssigneeSearchSelectPopover';
import { FakeInput } from 'tg.component/FakeInput';
import { User } from 'tg.component/UserAccount';
import { useUserName } from 'tg.component/common/UserName';
import { useTranslate } from '@tolgee/react';

const StyledClearButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

type Props = {
  value: User[];
  onChange?: (users: User[]) => void;
  label: React.ReactNode;
  sx?: SxProps;
  className?: string;
  projectId: number;
  disabled?: boolean;
  filters?: AssigneeFilters;
};

export const AssigneeSearchSelect: React.FC<Props> = ({
  value,
  onChange,
  label,
  sx,
  className,
  projectId,
  disabled,
  filters,
}) => {
  const theme = useTheme();
  const anchorEl = useRef<HTMLAnchorElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const getUserName = useUserName();
  const { t } = useTranslate();

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleClick = () => {
    if (!disabled) {
      setIsOpen(true);
    }
  };

  const handleSelectOrganization = async (users: User[]) => {
    onChange?.(users);
    setIsOpen(false);
  };

  const handleClearAssignees = () => {
    onChange?.([]);
  };

  return (
    <>
      <Box display="grid" {...{ sx, className }}>
        <TextField
          variant="outlined"
          value={value.map((u) => getUserName(u)).join(', ')}
          data-cy="assignee-select"
          minHeight={false}
          label={label}
          disabled={disabled}
          sx={{
            background: theme.palette.background.default,
          }}
          InputProps={{
            placeholder: t('assignee_select_unassigned'),
            onClick: handleClick,
            disabled: disabled,
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
                {Boolean(value.length && !disabled) && (
                  <StyledClearButton
                    size="small"
                    onClick={stopAndPrevent(handleClearAssignees)}
                    tabIndex={-1}
                  >
                    <XClose />
                  </StyledClearButton>
                )}
                <StyledClearButton
                  size="small"
                  onClick={handleClick}
                  tabIndex={-1}
                  sx={{ pointerEvents: 'none' }}
                  disabled={disabled}
                >
                  <ArrowDropDown />
                </StyledClearButton>
              </Box>
            ),
          }}
        />

        <AssigneeSearchSelectPopover
          open={isOpen}
          onClose={handleClose}
          selected={value}
          onSelect={handleSelectOrganization}
          anchorEl={anchorEl.current!}
          projectId={projectId}
          filters={filters}
        />
      </Box>
    </>
  );
};
