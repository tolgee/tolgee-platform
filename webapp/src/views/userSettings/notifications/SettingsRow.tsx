import React from 'react';
import { Box, styled, Switch, Tooltip, Typography } from '@mui/material';
import { components, operations } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { T } from '@tolgee/react';

const StyledSwitch = styled(Box)`
  text-align: center;
`;

type Props = {
  description: string;
  subdescription: string;
  channels: components['schemas']['NotificationSettingGroupModel'];
  disabledInApp?: boolean;
  disabledEmail?: boolean;
  afterChange: () => void;
};

export const SettingsRow: React.FC<Props> = ({
  description = '',
  subdescription = '',
  channels,
  disabledInApp = false,
  disabledEmail = false,
  afterChange = () => {},
}: Props) => {
  const saveMutation = useApiMutation({
    url: '/v2/notifications-settings',
    method: 'put',
  });

  const saveSettings = (
    channel: operations['putNotificationSetting']['parameters']['query']['channel'],
    enabled: boolean
  ) => {
    saveMutation.mutate(
      {
        query: {
          group: channels.group,
          channel: channel,
          enabled: enabled,
        },
      },
      {
        onSuccess() {
          messageService.success(
            <T keyName="User data - Successfully updated!" />
          );
          afterChange();
        },
      }
    );
  };

  const inAppEnabled = channels.channels.find(
    (it) => it.channel === 'IN_APP'
  )!.enabled;

  const emailEnabled = channels.channels.find(
    (it) => it.channel === 'EMAIL'
  )!.enabled;

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
        <Tooltip title={disabledInApp && 'Cannot be turned off'}>
          <StyledSwitch>
            <Switch
              checked={inAppEnabled}
              disabled={disabledInApp}
              onClick={() => saveSettings('IN_APP', !inAppEnabled)}
            />
          </StyledSwitch>
        </Tooltip>
      </Box>
      <Box>
        <Tooltip title={disabledEmail && 'Cannot be turned off'}>
          <StyledSwitch>
            <Switch
              checked={emailEnabled}
              disabled={disabledEmail}
              onClick={() => saveSettings('EMAIL', !emailEnabled)}
            />
          </StyledSwitch>
        </Tooltip>
      </Box>
    </>
  );
};
