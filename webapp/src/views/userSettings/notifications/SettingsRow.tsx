import React from 'react';
import { Box, styled, Switch, Tooltip, Typography } from '@mui/material';
import { components, operations } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { T, useTranslate } from '@tolgee/react';

const StyledSwitch = styled(Box)`
  text-align: center;
`;

type Props = {
  description: string;
  subdescription: string;
  group: operations['putNotificationSetting']['requestBody']['content']['application/json']['group'];
  channels: components['schemas']['NotificationSettingGroupModel'];
  disabledInApp?: boolean;
  disabledEmail?: boolean;
  afterChange: () => void;
};

function settingsToggleDataCy(
  group: Props['group'],
  channel: components['schemas']['NotificationSettingsRequest']['channel']
) {
  return `notifications-settings-${group}-${channel}`;
}

export const SettingsRow: React.FC<Props> = ({
  description = '',
  subdescription = '',
  group,
  channels,
  disabledInApp = false,
  disabledEmail = false,
  afterChange = () => {},
}: Props) => {
  const { t } = useTranslate();
  const saveMutation = useApiMutation({
    url: '/v2/notification-settings',
    method: 'put',
  });

  const saveSettings = (
    channel: operations['putNotificationSetting']['requestBody']['content']['application/json']['channel'],
    enabled: boolean
  ) => {
    saveMutation.mutate(
      {
        content: {
          'application/json': {
            group: group,
            channel: channel,
            enabled: enabled,
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

  const inAppEnabled = channels.inApp;
  const emailEnabled = channels.email;

  return (
    <>
      <Box>
        <Box>
          <Typography variant="body1">{description}</Typography>
        </Box>
        <Box>
          <Typography
            variant="body2"
            color={(theme) => theme.palette.text.secondary}
          >
            {subdescription}
          </Typography>
        </Box>
      </Box>
      <Box>
        <Tooltip
          title={
            disabledInApp &&
            t('settings_notifications_tooltip_cannot_be_turned_off')
          }
        >
          <StyledSwitch>
            <Switch
              checked={inAppEnabled}
              disabled={disabledInApp}
              onClick={() => saveSettings('IN_APP', !inAppEnabled)}
              data-cy={settingsToggleDataCy(group, 'IN_APP')}
            />
          </StyledSwitch>
        </Tooltip>
      </Box>
      <Box>
        <Tooltip
          title={
            disabledEmail &&
            t('settings_notifications_tooltip_cannot_be_turned_off')
          }
        >
          <StyledSwitch>
            <Switch
              checked={emailEnabled}
              disabled={disabledEmail}
              onClick={() => saveSettings('EMAIL', !emailEnabled)}
              data-cy={settingsToggleDataCy(group, 'EMAIL')}
            />
          </StyledSwitch>
        </Tooltip>
      </Box>
    </>
  );
};
