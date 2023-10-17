import React, { useEffect } from 'react';
import { Portal, keyframes, styled } from '@mui/material';
import { useWindowDimensions } from 'tg.hooks/useWindowDimensions';
import { useBottomPanelSetters } from './BottomPanelContext';

const fadeIn = keyframes`
  0% {
    opacity: 0;
    transform: translateY(100%);
  }
  100% {
    opacity: 1;
    transform: translateY(0%);
  },
`;

const StyledPopper = styled('div')`
  z-index: ${({ theme }) => theme.zIndex.modal};
  position: fixed;
  bottom: 0px;
  left: 0px;
  right: 0px;
  opacity: 1;
  animation: ${fadeIn} 0.2s ease-in-out 1;
`;

const StyledPopperContent = styled('div')`
  display: flex;
  background: ${({ theme }) => theme.palette.cell.selected};
  box-shadow: ${({ theme }) => theme.shadows[10]};
`;

type Props = {
  children: (width: number) => React.ReactNode;
  height: number;
};

export const BottomPanel: React.FC<Props> = ({ children, height }) => {
  const { width } = useWindowDimensions();

  const { setHeight } = useBottomPanelSetters();

  useEffect(() => {
    setHeight(height);
    return () => setHeight(0);
  }, [height]);

  return (
    <Portal>
      <StyledPopper role="dialog">
        <StyledPopperContent style={{ height }}>
          {children(width)}
        </StyledPopperContent>
      </StyledPopper>
    </Portal>
  );
};
