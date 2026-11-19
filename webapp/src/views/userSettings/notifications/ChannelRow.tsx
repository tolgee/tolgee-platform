import { ReactNode } from 'react';
import { Box, Switch, Tooltip, Typography } from '@mui/material';

export type Channel = 'inApp' | 'email';

export type ChannelState = { checked: boolean };

type Props = {
  label: ReactNode;
  description?: ReactNode;
  inApp: ChannelState;
  email: ChannelState;
  emailDisabledReason?: string;
  onChange: (channel: Channel, enabled: boolean) => void;
  dataCyType: string;
};

export const ChannelRow = ({
  label,
  description,
  inApp,
  email,
  emailDisabledReason,
  onChange,
  dataCyType,
}: Props) => {
  const cell = (channel: Channel, state: ChannelState, disabled?: string) => (
    <Box display="flex" justifyContent="flex-end" alignItems="center">
      <Tooltip title={disabled ?? ''}>
        <span>
          <Switch
            sx={{ mr: '-12px' }}
            checked={state.checked && !disabled}
            disabled={Boolean(disabled)}
            onChange={(e) => onChange(channel, e.target.checked)}
            data-cy="notifications-settings-localization-toggle"
            data-cy-type={dataCyType}
            data-cy-channel={channel === 'inApp' ? 'IN_APP' : 'EMAIL'}
          />
        </span>
      </Tooltip>
    </Box>
  );

  return (
    <>
      <Box alignSelf="center">
        <Typography variant="body1">{label}</Typography>
        {description && (
          <Typography variant="body2" color="text.secondary">
            {description}
          </Typography>
        )}
      </Box>
      {cell('inApp', inApp)}
      {cell('email', email, emailDisabledReason)}
    </>
  );
};
