import { default as React, FunctionComponent, useState } from 'react';
import {
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  styled,
} from '@mui/material';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import { useHistory } from 'react-router-dom';

const StyledMenu = styled(Menu)`
  .MuiPaper-root {
    margin-top: 5px;
  }
`;

const StyledIconButton = styled(IconButton)`
  width: 40px;
  height: 40px;

  img {
    user-drag: none;
  }
`;

export const Notifications: FunctionComponent<{ className?: string }> = () => {
  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    // @ts-ignore
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const [anchorEl, setAnchorEl] = useState(null);

  const history = useHistory();

  const notifications = [
    {
      id: 1,
      linkedEntityId: 1000000001,
      linkedEntityName: 'Task One',
    },
    {
      id: 2,
      linkedEntityId: 1000000002,
      linkedEntityName: 'Task Two',
    },
    {
      id: 3,
      linkedEntityId: 1000000003,
      linkedEntityName: 'Task Three',
    },
  ];

  return (
    <>
      <StyledIconButton
        color="inherit"
        aria-controls="language-menu"
        aria-haspopup="true"
        data-cy="global-language-menu"
        onClick={handleOpen}
        size="large"
      >
        🔔
      </StyledIconButton>
      <StyledMenu
        id="language-menu"
        keepMounted
        open={!!anchorEl}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        <List>
          {notifications.map((notification) => (
            <ListItemButton
              key={notification.id}
              divider
              onClick={() => {
                handleClose();
                history.push(
                  `/projects/1/languages/language/${notification.linkedEntityId}`
                );
              }}
            >
              <ListItemText
                primary={`A task has been assigned to you: ${notification.linkedEntityName}`}
              />
            </ListItemButton>
          ))}
        </List>
        {notifications.length === 0 && (
          <MenuItem>No notifications</MenuItem>
        )}
      </StyledMenu>
    </>
  );
};
