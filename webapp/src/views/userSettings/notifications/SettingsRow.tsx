import React from 'react';
import { Box, styled, Switch, Tooltip, Typography } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

const StyledSwitch = styled(Box)`
  text-align: center;
`;

type Props = {
  description: string;
  subdescription: string;
  settings: components['schemas']['NotificationSettingModel'];
  disabledInApp?: boolean;
  disabledEmail?: boolean;
};

export const SettingsRow: React.FC<Props> = ({
  description = '',
  subdescription = '',
  settings,
  disabledInApp = false,
  disabledEmail = false,
}: Props) => {
  const saveMutation = useApiMutation({
    url: '/v2/notifications-settings',
    method: 'put',
  });

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
              checked={settings.enabledForInApp}
              disabled={disabledInApp}
              onClick={() => {
                saveMutation.mutate({
                  query: {
                    group: settings.group,
                    channel: 'IN_APP',
                    enabled: !settings.enabledForInApp,
                  },
                });
                // TODO snackbar notifikaci
              }}
            />
          </StyledSwitch>
        </Tooltip>
      </Box>
      <Box>
        <Tooltip title={disabledEmail && 'Cannot be turned off'}>
          <StyledSwitch>
            <Switch
              checked={settings.enabledForEmail}
              disabled={disabledEmail}
              onClick={() => {
                saveMutation.mutate({
                  query: {
                    group: settings.group,
                    channel: 'EMAIL',
                    enabled: !settings.enabledForEmail,
                  },
                });
                // TODO snackbar notifikaci
              }}
            />
          </StyledSwitch>
        </Tooltip>
      </Box>
    </>
  );
};
