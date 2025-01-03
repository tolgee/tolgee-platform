import { default as React, FunctionComponent, useState } from 'react';
import {
  IconButton,
  List, ListItem,
  ListItemButton,
  ListItemText,
  styled,
} from '@mui/material';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import { useHistory } from 'react-router-dom';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { Bell01 } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

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
  const { t } = useTranslate();

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    // @ts-ignore
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const [anchorEl, setAnchorEl] = useState(null);

  const history = useHistory();

  const notifications = useApiQuery({
    url: '/v2/notifications',
    method: 'get',
  });

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
        <Bell01 />
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
          {notifications.data?.notifications.map((notification) => (
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
              <T
                keyName="notifications-task-assigned"
                params={{ taskName: notification.linkedTaskName }}
              />
            </ListItemButton>
          ))}
          {notifications?.length === 0 && (
            <ListItem>
              <T keyName="notifications-empty" />
            </ListItem>
          )}
        </List>
      </StyledMenu>
    </>
  );
};
