import React from 'react';
import { IconButton, Menu } from '@mui/material';

type IconMenuButtonProps = {
  icon: React.ReactNode;
  'data-cy'?: string;
  renderMenu: (close: () => void) => React.ReactNode;
};

export const IconMenu: React.FC<IconMenuButtonProps> = ({
  icon,
  renderMenu,
  ...iconButtonProps
}) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);

  return (
    <>
      <IconButton onClick={handleOpen} {...iconButtonProps}>
        {icon}
      </IconButton>

      <Menu anchorEl={anchorEl} open={open} onClose={handleClose}>
        {renderMenu(handleClose)}
      </Menu>
    </>
  );
};
