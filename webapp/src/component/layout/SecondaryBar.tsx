import React, { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';

const StyledBox = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

type Props = React.ComponentProps<typeof Box> & {
  noBorder?: boolean;
  reducedSpacing?: boolean;
};

export const SecondaryBar: FunctionComponent<Props> = ({
  noBorder,
  reducedSpacing,
  ...props
}) => (
  <StyledBox
    m={reducedSpacing ? 0 : 3}
    mb={0}
    mt={0}
    pb={2}
    pt={reducedSpacing ? 0 : 2}
    {...props}
    sx={{ borderBottom: noBorder ? 0 : undefined }}
  >
    {props.children}
  </StyledBox>
);
