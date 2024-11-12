import clsx from 'clsx';
import { useEffect, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { IconButton, Portal, styled, Tooltip } from '@mui/material';
import { ChevronUp } from '@untitled-ui/icons-react';
import { useDebouncedCallback } from 'use-debounce';

import { useTranslationsSelector } from './context/TranslationsContext';
import { BatchOperations } from './BatchOperations/BatchOperations';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

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

const StyledCounterContainer = styled('div')`
  display: flex;
  background: ${({ theme }) => theme.palette.background.paper};
  align-items: stretch;
  transition: opacity 0.3s ease-in-out;
  border-radius: 6px;
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  margin: ${({ theme }) => theme.spacing(2, 3, 2, 0)};
  flex-shrink: 0;
  white-space: nowrap;
  pointer-events: all;

  &.hidden {
    opacity: 0;
    pointer-events: none;
    width: 0px;
    overflow: hidden;
    margin-right: ${({ theme }) => theme.spacing(1)};
  }
`;

const StyledDivider = styled('div')`
  border-right: 1px solid ${({ theme }) => theme.palette.divider};
`;

const StyledIndex = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  margin-right: ${({ theme }) => theme.spacing(2)};
  margin-left: ${({ theme }) => theme.spacing(1)};
`;

const StyledIconButton = styled(IconButton)`
  flex-shrink: 0;
  width: 40px;
  height: 40px;
`;

const StyledStretcher = styled('div')`
  font-family: monospace;
  height: 0px;
  overflow: hidden;
`;

export const TranslationsToolbar: React.FC = () => {
  const [index, setIndex] = useState(1);
  const [isMouseOver, setIsMouseOver] = useState(false);
  const [selectionOpen, setSelectionOpen] = useState(false);
  const [toolbarVisible, setToolbarVisible] = useState(false);
  const { t } = useTranslate();
  const totalCount = useTranslationsSelector((c) => c.translationsTotal || 0);
  const list = useTranslationsSelector((c) => c.reactList);
  const selection = useTranslationsSelector((c) => c.selection);
  const getVisibleRange = list?.getVisibleRange.bind(list);

  const handleScrollUp = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const onScroll = useDebouncedCallback(
    () => {
      const [start, end] = getVisibleRange?.() || [0, 0];
      const fromBeginning = start;
      const toEnd = totalCount - 1 - end;
      const total = fromBeginning + toEnd || 1;
      const progress = (total - toEnd) / total;
      const newIndex = Math.round(progress * (totalCount - 1) + 1);
      setIndex(newIndex);
      setToolbarVisible(start > 0 && newIndex > 1);
    },
    100,
    { maxWait: 200 }
  );

  useEffect(() => {
    onScroll();
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, [getVisibleRange]);

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

  const counterContent = `${index} / ${totalCount}`;

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
        <StyledCounterContainer
          className={clsx({
            hidden: !toolbarVisible,
          })}
        >
          <StyledIndex>
            <span data-cy="translations-toolbar-counter">{counterContent}</span>
            {/* stretch content by monospace font, so it's not jumping */}
            <StyledStretcher>{counterContent}</StyledStretcher>
          </StyledIndex>
          <StyledDivider />
          <Tooltip title={t('translations_toolbar_to_top')} disableInteractive>
            <StyledIconButton
              data-cy="translations-toolbar-to-top"
              onClick={handleScrollUp}
              size="small"
              aria-label={t('translations_toolbar_to_top')}
            >
              <ChevronUp width={20} height={20} />
            </StyledIconButton>
          </Tooltip>
        </StyledCounterContainer>
      </StyledContainer>
    </Portal>
  );
};
