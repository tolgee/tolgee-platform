import React from 'react';
import { Box, styled, Switch, Tooltip, Typography } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';

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
            />
          </StyledSwitch>
        </Tooltip>
      </Box>
    </>
  );
};
