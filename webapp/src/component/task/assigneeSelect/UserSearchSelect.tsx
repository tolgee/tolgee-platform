import { useRef, useState } from 'react';
import {
  Box,
  styled,
  IconButton,
  InputBaseComponentProps,
  SxProps,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { ArrowDropDown, Clear } from '@mui/icons-material';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import { TextField } from 'tg.component/common/TextField';
import { UserSearchSelectPopover } from './UserSearchSelectPopover';
import { User } from './types';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledClearButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

const StyledFakeInput = styled('div')`
  padding: 8.5px 14px;
  height: 23px;
  box-sizing: content-box;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
`;

const StyledPlaceholder = styled('span')`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const FakeInput = ({ value, ...rest }: InputBaseComponentProps) => {
  const { t } = useTranslate();
  return (
    <StyledFakeInput tabIndex={0} {...(rest as any)}>
      {value || (
        <StyledPlaceholder>{t('assignee_placeholder')}</StyledPlaceholder>
      )}
    </StyledFakeInput>
  );
};

type Props = {
  value: User[];
  onChange?: (users: User[]) => void;
  label: React.ReactNode;
  sx?: SxProps;
  className?: string;
  project: SimpleProjectModel;
};

export const UserSearchSelect: React.FC<Props> = ({
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
          value={value.map((u) => u.name).join(', ')}
          data-cy="assignee-select"
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
                    onClick={stopAndPrevent(handleClearAssignees)}
                    tabIndex={-1}
                  >
                    <Clear fontSize="small" />
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

        <UserSearchSelectPopover
          open={isOpen}
          onClose={handleClose}
          selected={value}
          onSelect={handleSelectOrganization}
          anchorEl={anchorEl.current!}
          project={project}
        />
      </Box>
    </>
  );
};
