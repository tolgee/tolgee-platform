import { default as React, FunctionComponent, useState } from 'react';
import {
  Badge,
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

const ListItemHeader = styled(ListItem)`
  font-weight: bold;
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
  }).data;

  const notificationsData = notifications?._embedded?.notificationModelList;

  return (
    <>
      <StyledIconButton
        color="inherit"
        aria-controls="notifications-button"
        aria-haspopup="true"
        data-cy="notifications-button"
        onClick={handleOpen}
        size="large"
      >
        <Badge
          badgeContent={notifications?.page?.totalElements}
          color="secondary"
          data-cy="notifications-count"
        >
          <Bell01 />
        </Badge>
      </StyledIconButton>
      <StyledMenu
        id="notifications-list"
        data-cy="notifications-list"
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
        slotProps={{
          paper: {
            style: {
              maxHeight: 400,
            },
          },
        }}
      >
        <List>
          <ListItemHeader divider>
            <T keyName="notifications-header" />
          </ListItemHeader>
          {notificationsData?.map((notification, i) => {
            const destinationUrl = `/projects/${notification.project?.id}/task?number=${notification.linkedTask?.number}`;
            return (
              <ListItemButton
                key={notification.id}
                divider={i !== notificationsData.length - 1}
                href={destinationUrl}
                onClick={(event) => {
                  event.preventDefault();
                  handleClose();
                  history.push(destinationUrl);
                }}
              >
                <T
                  keyName="notifications-task-assigned"
                  params={{ taskName: notification.linkedTask?.name }}
                />
              </ListItemButton>
            );
          })}
          {notifications?.page?.totalElements === 0 && (
            <ListItem>
              <T keyName="notifications-empty" />
            </ListItem>
          )}
        </List>
      </StyledMenu>
    </>
  );
};
