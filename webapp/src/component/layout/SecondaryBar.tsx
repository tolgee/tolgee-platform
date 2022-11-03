import React, { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';

const StyledBox = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
`;

type Props = React.ComponentProps<typeof Box> & {
  noBorder?: boolean;
};

export const SecondaryBar: FunctionComponent<Props> = ({
  noBorder,
  ...props
}) => (
  <StyledBox
    m={3}
    mb={0}
    mt={0}
    pb={2}
    pt={2}
    {...props}
    sx={{ borderBottom: noBorder ? 0 : undefined }}
  >
    {props.children}
  </StyledBox>
);
