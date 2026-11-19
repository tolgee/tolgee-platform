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
          'In the morning, at midday and at the end of the day, your time.'
        );
      case 'DAILY':
        return t(
          'settings_notifications_email_hint_daily',
          'Every morning in your time zone, covering the last 24 hours.'
        );
      case 'OFF':
        return t(
          'settings_notifications_summary_hint_off',
          'No summaries. Mentions, tasks and security alerts still arrive by email.'
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
            {t('settings_notifications_summary_email', 'Summary email')}
          </Typography>
          <Tooltip
            title={
              <>
                {t(
                  'settings_notifications_summary_email_info_intro',
                  'Notifications with Email on, except mentions and tasks.'
                )}
                <Box component="ul" m={0} pl={2}>
                  <li>
                    {t(
                      'settings_notifications_summary_email_info_3_times',
                      '3 times a day: 9:00, 13:00, 17:00'
                    )}
                  </li>
                  <li>
                    {t(
                      'settings_notifications_summary_email_info_morning',
                      'Every morning: 9:00'
                    )}
                  </li>
                </Box>
                {t(
                  'settings_notifications_summary_email_info_outro',
                  'In your time zone, only when there is something new.'
                )}
              </>
            }
            disableInteractive
            componentsProps={{ tooltip: { style: { maxWidth: 400 } } }}
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
