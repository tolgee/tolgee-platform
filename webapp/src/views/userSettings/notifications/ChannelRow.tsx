import { ReactNode } from 'react';
import { Box, Switch, Tooltip, Typography } from '@mui/material';
import { InfoCircle } from '@untitled-ui/icons-react';

export type Channel = 'inApp' | 'email';

export type ChannelState = { checked: boolean };

type Props = {
  label: ReactNode;
  description?: ReactNode;
  hint?: string;
  inApp?: ChannelState;
  email?: ChannelState;
  single?: ChannelState;
  emailNote?: ReactNode;
  emailDisabledReason?: string;
  onChange: (channel: Channel, enabled: boolean) => void;
  dataCyType: string;
};

export const ChannelRow = ({
  label,
  description,
  hint,
  inApp,
  email,
  single,
  emailNote,
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
        <Box display="flex" alignItems="center" gap={0.75}>
          <Typography variant="body1">{label}</Typography>
          {hint && (
            <Tooltip title={hint} disableInteractive>
              <Box display="flex" color="text.secondary">
                <InfoCircle width={16} height={16} />
              </Box>
            </Tooltip>
          )}
        </Box>
        {description && (
          <Typography variant="body2" color="text.secondary">
            {description}
          </Typography>
        )}
      </Box>
      {single ? (
        <>
          {cell('inApp', single)}
          <Box
            display="flex"
            justifyContent="flex-end"
            alignItems="center"
            fontSize={13}
            color="text.secondary"
            whiteSpace="nowrap"
          >
            {emailNote}
          </Box>
        </>
      ) : (
        <>
          {inApp && cell('inApp', inApp)}
          {email && cell('email', email, emailDisabledReason)}
        </>
      )}
    </>
  );
};
