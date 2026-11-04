import { Box, styled, Typography } from '@mui/material';
import { FC, ReactNode } from 'react';
import { KeyHeader, KeyWrapper } from './KeyPanelBase';

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

export const PlaceholderKeyPanel: FC<
  React.PropsWithChildren<{ text: string; headerStart?: ReactNode }>
> = ({ text, headerStart }) => (
  <KeyWrapper>
    {headerStart && (
      <KeyHeader>
        <Box display="flex">{headerStart}</Box>
      </KeyHeader>
    )}
    <PlaceholderContent>
      <Typography variant="body2" sx={{ fontStyle: 'italic' }}>
        {text}
      </Typography>
    </PlaceholderContent>
  </KeyWrapper>
);
