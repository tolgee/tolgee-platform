import React, { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';

const StyledBox = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
`;

export const SecondaryBar: FunctionComponent<React.ComponentProps<typeof Box>> =
  (props) => (
    <StyledBox m={3} mb={0} mt={0} pb={2} pt={2} {...props}>
      {props.children}
    </StyledBox>
  );
