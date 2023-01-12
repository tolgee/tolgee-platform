import { Popover } from '@mui/material';

type Props = {
  anchorEl: HTMLElement;
  open: boolean;
  onClose: () => void;
};

export const SearchSelectPopover: React.FC<Props> = ({
  anchorEl,
  open,
  onClose,
  children,
}) => {
  return (
    <Popover
      anchorEl={anchorEl}
      open={open}
      onClose={onClose}
      disablePortal={false}
      anchorOrigin={{
        vertical: 'top',
        horizontal: 'center',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'center',
      }}
    >
      {children}
    </Popover>
  );
};
