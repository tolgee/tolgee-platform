import { Box, styled } from '@mui/material';
import React from 'react';
import clsx from 'clsx';

const StyledRoot = styled(Box)`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1;

  opacity: 0;
  pointer-events: none;

  transition: opacity 0.2s ease-in-out;

  background-color: ${({ theme }) => theme.palette.background.default};

  &.visible {
    pointer-events: all;
    opacity: 0.6;
  }
`;

export const ImportResultLoadingOverlay = (props: { loading: boolean }) => {
  return <StyledRoot className={clsx({ visible: props.loading })} />;
};
