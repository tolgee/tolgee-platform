import React from 'react';
import { Box, styled, Switch, Tooltip, Typography } from '@mui/material';

const StyledSwitch = styled(Box)`
  text-align: center;
`;

type Props = {
  description: string;
  subdescription: string;
  disabledInApp?: boolean;
  disabledEmail?: boolean;
};

export const SettingsRow: React.FC<Props> = ({
  description = '',
  subdescription = '',
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
            <Switch defaultChecked disabled={disabledInApp} />
          </StyledSwitch>
        </Tooltip>
      </Box>
      <Box>
        <Tooltip title={disabledEmail && 'Cannot be turned off'}>
          <StyledSwitch>
            <Switch defaultChecked disabled={disabledEmail} />
          </StyledSwitch>
        </Tooltip>
      </Box>
    </>
  );
};
