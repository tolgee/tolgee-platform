import { Box, styled, Typography } from '@mui/material';
import { FC } from 'react';
import { KeyPanel } from './KeyPanelBase';

const PlaceholderContent = styled(Box)`
  min-height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: ${({ theme }) => theme.spacing(2)};
  color: ${({ theme }) => theme.palette.text.secondary};
  background: ${({ theme }) => theme.palette.action.hover};
  text-align: center;
  flex: 1;
`;

export const PlaceholderKeyPanel: FC<{ text: string }> = ({ text }) => (
  <KeyPanel>
    <PlaceholderContent>
      <Typography variant="body2" sx={{ fontStyle: 'italic' }}>
        {text}
      </Typography>
    </PlaceholderContent>
  </KeyPanel>
);
