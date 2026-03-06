import clsx from 'clsx';
import { useEffect, useRef, useState } from 'react';
import { Portal, styled } from '@mui/material';

import { useTranslationsSelector } from './context/TranslationsContext';
import { BatchOperations } from './BatchOperations/BatchOperations';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useScrollCounter } from './useScrollCounter';
import { FloatingScrollCounter } from './FloatingScrollCounter';
import { ReactList } from 'tg.component/reactList/ReactList';

const StyledContainer = styled('div')`
  z-index: ${({ theme }) => theme.zIndex.drawer};
  position: fixed;
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  bottom: 0px;
  padding-left: 44px;
  pointer-events: none;
`;

const StyledShortcutsContainer = styled('div')`
  flex-grow: 1;
  margin: ${({ theme }) => theme.spacing(2, 1, 2, 3)};
  flex-shrink: 1;
  flex-basis: 1px;
  position: relative;
`;

const StyledCounterWrapper = styled('div')`
  flex-shrink: 0;

  &.hidden {
    width: 0px;
    overflow: hidden;
    margin-right: ${({ theme }) => theme.spacing(1)};
  }
`;

export const TranslationsToolbar: React.FC = () => {
  const [isMouseOver, setIsMouseOver] = useState(false);
  const [selectionOpen, setSelectionOpen] = useState(false);
  const totalCount = useTranslationsSelector((c) => c.translationsTotal || 0);
  const list = useTranslationsSelector((c) => c.reactList);
  const selection = useTranslationsSelector((c) => c.selection);

  const reactListRef = useRef<ReactList | null>(null);
  reactListRef.current = list ?? null;

  const { scrollIndex, toolbarVisible, handleScrollUp } = useScrollCounter(
    reactListRef,
    totalCount
  );

  const handlePointerEnter = () => {
    setIsMouseOver(true);
  };
  const handlePointerLeave = () => {
    setIsMouseOver(false);
  };

  useEffect(() => {
    if (selection.length || (selectionOpen && isMouseOver)) {
      setSelectionOpen(true);
    } else {
      setSelectionOpen(false);
    }
  }, [isMouseOver, Boolean(selection.length)]);

  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);

  return (
    <Portal>
      <StyledContainer
        onPointerEnter={handlePointerEnter}
        onPointerLeave={handlePointerLeave}
        style={{ width: `calc(100vw - ${rightPanelWidth}px)` }}
      >
        <StyledShortcutsContainer>
          <BatchOperations
            open={selectionOpen}
            onClose={() => setIsMouseOver(false)}
          />
        </StyledShortcutsContainer>
        <StyledCounterWrapper className={clsx({ hidden: !toolbarVisible })}>
          <FloatingScrollCounter
            scrollIndex={scrollIndex}
            totalCount={totalCount}
            visible={toolbarVisible}
            onScrollUp={handleScrollUp}
            dataCyPrefix="translations"
          />
        </StyledCounterWrapper>
      </StyledContainer>
    </Portal>
  );
};
