import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { Bell01 } from '@untitled-ui/icons-react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import {
  LocalizationActionLabel,
  LocalizationCountLabel,
} from 'tg.component/layout/Notifications/LocalizationActionLabel';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';

import { PrefilterContainer } from 'tg.views/projects/translations/prefilters/ContainerPrefilter';

const StyledContent = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  overflow: hidden;
  white-space: nowrap;
`;

const StyledSecondary = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  notificationId: number;
};

export const PrefilterNotification = ({ notificationId }: Props) => {
  const timeDistance = useTimeDistance();

  const notificationLoadable = useApiQuery({
    url: '/v2/notification/{notificationId}',
    method: 'get',
    path: { notificationId },
    options: {
      retry: false,
    },
    fetchOptions: {
      disableAutoErrorHandle: true,
    },
  });

  const notification = notificationLoadable.data;
  const shownAt = notification?.updatedAt ?? notification?.createdAt;

  if (notificationLoadable.isError) {
    return (
      <PrefilterContainer
        icon={<Bell01 width={20} height={20} />}
        title={
          <T
            keyName="notification_filter_indicator_not_available"
            defaultValue="This notification is not available to you"
          />
        }
        content={
          <StyledSecondary data-cy="translations-notification-prefilter-unavailable">
            <T
              keyName="notification_filter_indicator_not_available_hint"
              defaultValue="It was sent to someone else or it no longer exists. Clear the filter to see all strings."
            />
          </StyledSecondary>
        }
      />
    );
  }

  return (
    <PrefilterContainer
      icon={<Bell01 width={20} height={20} />}
      title={
        <T
          keyName="notification_filter_indicator_title_plural"
          defaultValue="Notifications"
        />
      }
      content={
        notification && (
          <StyledContent data-cy="translations-notification-prefilter-detail">
            <Box>
              <LocalizationActionLabel
                type={notification.type}
                name={notification.originatingUser?.name ?? ''}
              />
            </Box>
            {notification.entityCount != null && (
              <Box>
                <LocalizationCountLabel
                  type={notification.type}
                  count={notification.entityCount}
                />
              </Box>
            )}
            {notification.languages?.map((language) => (
              <FlagImage
                key={language.id}
                flagEmoji={language.flagEmoji ?? ''}
                height={16}
              />
            ))}
            {shownAt && (
              <StyledSecondary>{timeDistance(shownAt)}</StyledSecondary>
            )}
          </StyledContent>
        )
      }
    />
  );
};
