import { Box, MenuItem, Select, Tooltip, Typography } from '@mui/material';
import { InfoCircle } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';

type Frequency =
  components['schemas']['NotificationSettingModel']['digestFrequency'];

type Props = {
  value: Frequency;
  afterChange: () => void;
};

export const WorkEmailFrequencyRow = ({ value, afterChange }: Props) => {
  const { t } = useTranslate();
  const mutation = useApiMutation({
    url: '/v2/notification-settings/digest-frequency',
    method: 'put',
  });

  const change = (next: Frequency) => {
    mutation.mutate(
      {
        content: {
          'application/json': {
            frequency: next,
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
    );
  };

  const hint = () => {
    switch (value) {
      case 'SOON':
        return t(
          'settings_notifications_email_hint_3_times_a_day',
          'Everything turned on above, sent at 9:00, 13:00 and 17:00 your time.'
        );
      case 'DAILY':
        return t(
          'settings_notifications_email_hint_daily',
          'Everything turned on above from the last 24 hours, sent at 9:00 your time.'
        );
      case 'OFF':
        return t(
          'settings_notifications_summary_hint_off',
          'Updates stay in the app only. Mentions and tasks are still emailed right away.'
        );
    }
  };

  return (
    <Box
      sx={{
        gridColumn: '1 / -1',
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        columnGap: 6,
        rowGap: 1,
      }}
    >
      <Box sx={{ flex: '1 1 300px' }}>
        <Box display="flex" alignItems="center" gap={0.5}>
          <Typography variant="body1">
            {t('settings_notifications_summary_frequency', 'Frequency')}
          </Typography>
          <Tooltip
            title={t(
              'settings_notifications_summary_email_info',
              'Sent only when there is something new. Mentions and tasks are emailed right away.'
            )}
            disableInteractive
          >
            <Box display="flex" color="text.secondary">
              <InfoCircle width={16} height={16} />
            </Box>
          </Tooltip>
        </Box>
        <Typography variant="body2" color="text.secondary">
          {hint()}
        </Typography>
      </Box>
      <Box sx={{ flex: '0 0 auto', marginLeft: 'auto' }}>
        <Select
          size="small"
          value={value}
          data-cy="notifications-settings-digest-frequency"
          sx={{ minWidth: (theme) => theme.spacing(25) }}
          onChange={(e) => change(e.target.value as Frequency)}
        >
          <MenuItem
            value="SOON"
            data-cy="notifications-settings-digest-frequency-option"
            data-cy-option="SOON"
          >
            {t('settings_notifications_summary_3_times_a_day', '3 times a day')}
          </MenuItem>
          <MenuItem
            value="DAILY"
            data-cy="notifications-settings-digest-frequency-option"
            data-cy-option="DAILY"
          >
            {t('settings_notifications_digest_every_morning', 'Every morning')}
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
    </Box>
  );
};
