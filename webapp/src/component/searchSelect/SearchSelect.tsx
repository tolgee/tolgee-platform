import * as React from 'react';
import PropTypes from 'prop-types';
import {
  ClickAwayListener,
  Autocomplete,
  autocompleteClasses,
  InputBase,
  Popover,
  MenuItem,
  styled,
} from '@mui/material';

const StyledAutocompletePopper = styled('div')(({ theme }) => ({
  [`& .${autocompleteClasses.paper}`]: {
    boxShadow: 'none',
    margin: 0,
    color: 'inherit',
    fontSize: 13,
  },
  [`& .${autocompleteClasses.listbox}`]: {
    backgroundColor: theme.palette.mode === 'light' ? '#fff' : '#1c2128',
    padding: 0,
    [`& .${autocompleteClasses.option}`]: {
      minHeight: 'auto',
      alignItems: 'flex-start',
      padding: 8,
      borderBottom: `1px solid  ${
        theme.palette.mode === 'light' ? ' #eaecef' : '#30363d'
      }`,
      '&[aria-selected="true"]': {
        backgroundColor: 'transparent',
      },
      [`&.${autocompleteClasses.focused}, &.${autocompleteClasses.focused}[aria-selected="true"]`]:
        {
          backgroundColor: theme.palette.action.hover,
        },
    },
  },
  [`&.${autocompleteClasses.popperDisablePortal}`]: {
    position: 'relative',
  },
}));

function PopperComponent(props) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disablePortal, anchorEl, open, ...other } = props;
  return <StyledAutocompletePopper {...other} />;
}

PopperComponent.propTypes = {
  anchorEl: PropTypes.any,
  disablePortal: PropTypes.bool,
  open: PropTypes.bool.isRequired,
};

const StyledInput = styled(InputBase)(({ theme }) => ({
  width: '100%',
  borderBottom: `1px solid ${
    theme.palette.mode === 'light' ? '#eaecef' : '#30363d'
  }`,
  '& input': {
    borderRadius: 4,
    backgroundColor: theme.palette.mode === 'light' ? '#fff' : '#0d1117',
    padding: 8,
    transition: theme.transitions.create(['border-color', 'box-shadow']),
    border: `1px solid ${
      theme.palette.mode === 'light' ? '#eaecef' : '#30363d'
    }`,
    fontSize: 14,
    '&:focus': {
      boxShadow: `0px 0px 0px 3px ${
        theme.palette.mode === 'light'
          ? 'rgba(3, 102, 214, 0.3)'
          : 'rgb(12, 45, 107)'
      }`,
      borderColor: theme.palette.mode === 'light' ? '#0366d6' : '#388bfd',
    },
  },
}));

type OrganizationItem = {
  name: string;
  id: number;
};

type Props = {
  anchorEl: HTMLElement | undefined;
  onClose?: () => void;
  onSelect: (value: any) => void;
  items: OrganizationItem[];
  selected: OrganizationItem;
};

export const SearchSelect: React.FC<Props> = ({
  anchorEl,
  onClose,
  onSelect,
  items,
  selected,
}) => {
  const handleClose = () => {
    if (anchorEl) {
      anchorEl?.focus?.();
    }
    onClose?.();
  };

  const open = Boolean(anchorEl);
  const id = open ? 'github-label' : undefined;

  return (
    <Popover id={id} open={open} anchorEl={anchorEl}>
      <ClickAwayListener onClickAway={handleClose}>
        <div>
          <Autocomplete
            onClose={() => {
              handleClose();
            }}
            onChange={(event, newValue, reason) => {
              if (
                event.type === 'keydown' &&
                // @ts-ignore
                event.key === 'Backspace' &&
                reason === 'removeOption'
              ) {
                return;
              }
              onSelect?.(newValue);
              onClose?.();
            }}
            PopperComponent={PopperComponent}
            renderTags={() => null}
            noOptionsText="No labels"
            renderOption={(props, option) => (
              <MenuItem selected={selected.id === option.id} {...props}>
                {option.name}
              </MenuItem>
            )}
            options={items}
            getOptionLabel={(option) => option.name}
            renderInput={(params) => (
              <StyledInput
                ref={params.InputProps.ref}
                inputProps={params.inputProps}
                autoFocus
                placeholder="Filter organizations"
              />
            )}
          />
        </div>
      </ClickAwayListener>
    </Popover>
  );
};
