import { FunctionComponent, ReactNode } from 'react';
import { Box, styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { getNotificationTranslationsUrl } from 'tg.constants/links';
import { LocalizationCountLabel } from 'tg.component/layout/Notifications/LocalizationActionLabel';

const StyledDetail = styled(Box)`
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
`;

const StyledCount = styled(Box)`
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledFlags = styled(Box)`
  display: flex;
  align-items: center;
  gap: 4px;
`;

const MAX_FLAGS = 4;

const StyledMoreLanguages = styled(Box)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledBranches = styled(Box)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = NotificationItemProps & { label: ReactNode };

export const LocalizationItem: FunctionComponent<Props> = ({
  notification,
  label,
  ...props
}) => {
  const { t } = useTranslate();
  const projectId = notification.project!.id;
  const languages = notification.languages ?? [];
  const shownLanguages = languages.slice(0, MAX_FLAGS);
  const hiddenLanguages = languages.slice(MAX_FLAGS);
  const destinationUrl = getNotificationTranslationsUrl(
    projectId,
    notification.id,
    notification.languages?.map((l) => l.tag)
  );

  return (
    <NotificationItem
      notification={notification}
      destinationUrl={destinationUrl}
      {...props}
    >
      <Box data-cy="notifications-localization-item">
        <Box data-cy="notifications-localization-label">{label}</Box>
        <StyledDetail>
          {notification.entityCount != null && (
            <StyledCount data-cy="notifications-localization-count">
              <LocalizationCountLabel
                type={notification.type}
                count={notification.entityCount}
              />
            </StyledCount>
          )}
          <StyledFlags>
            {shownLanguages.map((language) => (
              <Box
                key={language.id}
                data-cy="notifications-localization-flag"
                data-cy-tag={language.tag}
              >
                <FlagImage
                  flagEmoji={language.flagEmoji ?? ''}
                  height={16}
                  style={{ marginBottom: -3 }}
                />
              </Box>
            ))}
            {hiddenLanguages.length > 0 && (
              <Tooltip
                title={languages.map((l) => l.name).join(', ')}
                placement="bottom-start"
              >
                <StyledMoreLanguages data-cy="notifications-localization-more-languages">
                  {t('notifications-more-languages', '+{count}', {
                    count: hiddenLanguages.length,
                  })}
                </StyledMoreLanguages>
              </Tooltip>
            )}
          </StyledFlags>
          {Boolean(notification.branches?.length) && (
            <StyledBranches data-cy="notifications-localization-branches">
              {t('notifications-branches', 'branches: {names}', {
                names: notification.branches!.join(', '),
              })}
            </StyledBranches>
          )}
        </StyledDetail>
      </Box>
    </NotificationItem>
  );
};
