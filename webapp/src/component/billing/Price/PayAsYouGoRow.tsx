import { Box, styled } from '@mui/material';
import React from 'react';

type Props = React.ComponentProps<typeof Box> & {
  firstPart: React.ReactNode;
  secondPart: React.ReactNode;
};

const StyledSecondaryPrice = styled(Box)`
  display: flex;
  justify-content: space-between;
  gap: 20px;
`;

export const PayAsYouGoRow = ({ firstPart, secondPart, ...rest }: Props) => {
  return (
    <StyledSecondaryPrice {...rest}>
      <Box justifySelf="end">{firstPart}</Box>
      <Box justifySelf="start">{secondPart}</Box>
    </StyledSecondaryPrice>
  );
};
