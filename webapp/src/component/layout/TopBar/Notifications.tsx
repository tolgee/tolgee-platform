import { default as React, FunctionComponent, useState } from 'react';
import {
  IconButton,
  List,
  ListItem,
  ListItemButton,
  styled,
} from '@mui/material';
import Menu from '@mui/material/Menu';
import { useHistory } from 'react-router-dom';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { Bell01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

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

  const notifications = useApiQuery({
    url: '/v2/notifications',
    method: 'get',
    query: { size: 10000 },
  }).data?._embedded?.notificationModelList;

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
          {notifications?.map((notification, i) => (
            <ListItemButton
              key={notification.id}
              divider={i !== notifications.length - 1}
              onClick={() => {
                handleClose();
                history.push(
                  `/projects/${notification.projectId}/task?number=${notification.linkedTask?.number}`
                );
              }}
            >
              <T
                keyName="notifications-task-assigned"
                params={{ taskName: notification.linkedTask?.name }}
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
