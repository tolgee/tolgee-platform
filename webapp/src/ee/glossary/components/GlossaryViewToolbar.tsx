import { Box, Portal, styled } from '@mui/material';
import { GlossaryBatchToolbar } from 'tg.ee.module/glossary/components/GlossaryBatchToolbar';
import React from 'react';

const StyledBatchToolbarWrapper = styled(Box)`
  position: fixed;
  bottom: 0;
  z-index: ${({ theme }) => theme.zIndex.drawer};
`;

type Props = {
  leftOffset?: number;
} & React.ComponentProps<typeof GlossaryBatchToolbar>;

export const GlossaryViewToolbar = ({ leftOffset, ...toolbarProps }: Props) => {
  return (
    <Portal>
      <StyledBatchToolbarWrapper
        sx={{
          left: leftOffset,
        }}
      >
        <GlossaryBatchToolbar {...toolbarProps} />
      </StyledBatchToolbarWrapper>
    </Portal>
  );
};
