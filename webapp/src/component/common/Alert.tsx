import { default as React, FunctionComponent } from 'react';
import { styled } from '@mui/material';
import MuiAlert from '@mui/material/Alert';

const StyledMuiAlert = styled(MuiAlert)`
  margin-top: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
`;

export const Alert: FunctionComponent<React.ComponentProps<typeof MuiAlert>> = (
  props
) => {
  return <StyledMuiAlert {...props} />;
};
