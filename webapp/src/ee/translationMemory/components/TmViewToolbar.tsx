import React from 'react';
import { Box, Portal, styled } from '@mui/material';
import { TmBatchToolbar } from './TmBatchToolbar';

const StyledBatchToolbarWrapper = styled(Box)`
  position: fixed;
  bottom: 0;
  z-index: ${({ theme }) => theme.zIndex.drawer};
`;

type Props = {
  leftOffset?: number;
} & React.ComponentProps<typeof TmBatchToolbar>;

export const TmViewToolbar = ({ leftOffset, ...toolbarProps }: Props) => {
  return (
    <Portal>
      <StyledBatchToolbarWrapper sx={{ left: leftOffset }}>
        <TmBatchToolbar {...toolbarProps} />
      </StyledBatchToolbarWrapper>
    </Portal>
  );
};
