import { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { LINKS, PARAMS } from 'tg.constants/links';

const ENTITY_CAP = 100;

const StyledFlag = styled(Box)`
  display: inline;
  margin-right: 4px;
`;

const StyledCount = styled(Box)`
  display: inline;
  margin-left: 6px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledBranches = styled(Box)`
  display: inline;
  margin-left: 8px;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = NotificationItemProps & { label: string };

export const LocalizationItem: FunctionComponent<Props> = ({
  notification,
  label,
  ...props
}) => {
  const { t } = useTranslate();
  const projectId = notification.project!.id;
  const toActivity =
    notification.type === 'AUTOMATICALLY_TRANSLATED' ||
    notification.type === 'BULK_CHANGED';
  const destinationUrl = toActivity
    ? LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: projectId })
    : LINKS.PROJECT_TRANSLATIONS.build({ [PARAMS.PROJECT_ID]: projectId });
  const count = notification.entityCount;

  return (
    <NotificationItem
      notification={notification}
      destinationUrl={destinationUrl}
      {...props}
    >
      <Box data-cy="notifications-localization-item">
        <Box>
          <b>{label}</b>
        </Box>
        <Box>
          {notification.languages?.map((language) => (
            <StyledFlag
              key={language.id}
              data-cy="notifications-localization-flag"
              data-cy-tag={language.tag}
            >
              <FlagImage
                flagEmoji={language.flagEmoji ?? ''}
                height={16}
                style={{ marginBottom: -3 }}
              />
            </StyledFlag>
          ))}
          {count !== undefined && count !== null && (
            <StyledCount data-cy="notifications-localization-count">
              {count >= ENTITY_CAP
                ? t('notifications-count-capped', '100+')
                : count}
            </StyledCount>
          )}
          {Boolean(notification.branches?.length) && (
            <StyledBranches data-cy="notifications-localization-branches">
              {t('notifications-branches', 'branches: {names}', {
                names: notification.branches!.join(', '),
              })}
            </StyledBranches>
          )}
        </Box>
      </Box>
    </NotificationItem>
  );
};
