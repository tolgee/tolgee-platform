import { Box, MenuItem, Select, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';

type Frequency =
  components['schemas']['NotificationSettingModel']['digestFrequency'];

type Props = { value: Frequency; afterChange: () => void };

export const WorkEmailFrequencyRow = ({ value, afterChange }: Props) => {
  const { t } = useTranslate();
  const mutation = useApiMutation({
    url: '/v2/notification-settings/digest-frequency',
    method: 'put',
  });

  return (
    <>
      <Box>
        <Typography variant="body1">
          {t('settings_notifications_work_emails_label', 'Send work emails')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t(
            'settings_notifications_work_emails_hint',
            'Mentions always arrive soon. We send at most one work email every 4 hours.'
          )}
        </Typography>
      </Box>
      <Box
        sx={{
          gridColumn: '2 / 4',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        <Select
          size="small"
          value={value}
          data-cy="notifications-settings-digest-frequency"
          sx={{ minWidth: (theme) => theme.spacing(25) }}
          onChange={(e) =>
            mutation.mutate(
              {
                content: {
                  'application/json': {
                    frequency: e.target.value as Frequency,
                  },
                },
              },
              {
                onSuccess() {
                  messageService.success(
                    <T keyName="settings_notifications_message_saved" />
                  );
                  afterChange();
                },
              }
            )
          }
        >
          <MenuItem
            value="SOON"
            data-cy="notifications-settings-digest-frequency-option"
            data-cy-option="SOON"
          >
            {t('settings_notifications_digest_soon', 'Soon after changes')}
          </MenuItem>
          <MenuItem
            value="DAILY"
            data-cy="notifications-settings-digest-frequency-option"
            data-cy-option="DAILY"
          >
            {t('settings_notifications_digest_daily', 'Once a day')}
          </MenuItem>
          <MenuItem
            value="OFF"
            data-cy="notifications-settings-digest-frequency-option"
            data-cy-option="OFF"
          >
            {t('settings_notifications_digest_off', 'Off')}
          </MenuItem>
        </Select>
      </Box>
    </>
  );
};
