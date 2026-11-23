import { FunctionComponent, ReactNode } from 'react';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { LINKS } from 'tg.constants/links';
import { useUser } from 'tg.globalContext/helpers';

const StyledAvatar = styled(Box)`
  position: relative;
  width: 32px;
  height: 32px;
`;

const StyledBadge = styled(Box)`
  position: absolute;
  right: -4px;
  bottom: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.palette.primary.main};
  background: ${({ theme }) => theme.palette.background.paper};
  box-shadow: 0 0 0 1px ${({ theme }) => theme.palette.divider};

  svg {
    width: 12px;
    height: 12px;
  }
`;

type Props = NotificationItemProps & {
  icon: ReactNode;
  renderLabel: (name: string) => ReactNode;
};

export const SecurityItem: FunctionComponent<Props> = ({
  notification,
  icon,
  renderLabel,
  ...props
}) => {
  const { t } = useTranslate();
  const user = useUser();
  const actor = notification.originatingUser;
  const name =
    actor && actor.id === user?.id
      ? t('notifications-security-actor-you', 'You')
      : actor?.name ?? t('notifications-unknown-author', 'Someone');

  return (
    <NotificationItem
      notification={notification}
      destinationUrl={LINKS.USER_ACCOUNT_SECURITY.build()}
      icon={
        <StyledAvatar>
          {actor && (
            <AvatarImg
              owner={{
                name: actor.name,
                avatar: actor.avatar,
                type: 'USER',
                id: actor.id || 0,
              }}
              size={32}
            />
          )}
          <StyledBadge>{icon}</StyledBadge>
        </StyledAvatar>
      }
      {...props}
    >
      <Box data-cy="notifications-security-label">{renderLabel(name)}</Box>
    </NotificationItem>
  );
};
