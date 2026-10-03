import { Box, Switch, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';

type Setting = components['schemas']['NotificationTypeSettingModel'];
type Channel = 'IN_APP' | 'EMAIL';

type Props = {
  setting: Setting;
  label: string;
  afterChange: () => void;
};

export const LocalizationSettingsRow = ({
  setting,
  label,
  afterChange,
}: Props) => {
  const saveMutation = useApiMutation({
    url: '/v2/notification-settings',
    method: 'put',
  });

  const save = (channel: Channel, enabled: boolean) => {
    saveMutation.mutate(
      {
        content: {
          'application/json': {
            group: 'LOCALIZATION',
            type: setting.type,
            channel,
            enabled,
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

  return (
    <>
      <Box>
        <Typography variant="body1">{label}</Typography>
      </Box>
      <Box textAlign="center">
        <Switch
          checked={setting.inApp}
          onChange={(e) => save('IN_APP', e.target.checked)}
          data-cy="notifications-settings-localization-toggle"
          data-cy-type={setting.type}
          data-cy-channel="IN_APP"
        />
      </Box>
      <Box textAlign="center">
        <Switch
          checked={setting.email}
          disabled={!setting.inApp}
          onChange={(e) => save('EMAIL', e.target.checked)}
          data-cy="notifications-settings-localization-toggle"
          data-cy-type={setting.type}
          data-cy-channel="EMAIL"
        />
      </Box>
    </>
  );
};
