import React, { useCallback, useRef, useState, useEffect } from 'react';
import {
  IconButton,
  Portal,
  styled,
  Tooltip,
  useMediaQuery,
} from '@mui/material';
import { ChevronLeft, ChevronRight, ChevronUp } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import clsx from 'clsx';
import { useDebouncedCallback } from 'use-debounce';
import { useScrollStatus } from 'tg.component/common/useScrollStatus';
import { ReactList } from 'tg.component/reactList/ReactList';
import { CellLanguage } from '../TranslationsTable/CellLanguage';
import { ColumnResizer } from '../ColumnResizer';
import { TrashRow } from './TrashRow';
import { TrashedKeyModel } from './TrashRow';
import { components } from 'tg.service/apiSchema.generated';

type LanguageModel = components['schemas']['LanguageModel'];

const HEADER_HEIGHT = 39;
const NAMESPACE_BANNER_SPACING = 14;
const ARROW_SIZE = 50;
const TRASHED_COLUMN_WIDTH = 200;

const StyledTableContainer = styled('div')`
  position: relative;
  display: grid;
  background: ${({ theme }) => theme.palette.background.default};
  flex-grow: 1;
  padding-bottom: 100px;

  &::before {
    content: '';
    height: 100%;
    position: absolute;
    width: 6px;
    background-image: linear-gradient(90deg, #0000002c, transparent);
    top: 0px;
    left: 0px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 100ms ease-in-out;
  }

  &::after {
    content: '';
    height: 100%;
    position: absolute;
    width: 6px;
    background-image: linear-gradient(-90deg, #0000002c, transparent);
    top: 0px;
    right: 0px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 100ms ease-in-out;
  }

  &.scrollLeft {
    &::before {
      opacity: 1;
    }
  }

  &.scrollRight {
    &::after {
      opacity: 1;
    }
  }
`;

const StyledVerticalScroll = styled('div')`
  overflow-x: auto;
  scrollbar-width: none;
  overflow-y: hidden;
  scroll-behavior: smooth;
`;

const StyledScrollArrow = styled('div')`
  position: fixed;
  top: 50vh;
  width: ${ARROW_SIZE / 2}px;
  height: ${ARROW_SIZE}px;
  z-index: 5;
  cursor: pointer;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  background: ${({ theme }) => theme.palette.background.default};
  opacity: 0;
  transition: opacity 150ms ease-in-out;
  pointer-events: none;

  display: flex;
  align-items: center;
  justify-content: center;

  &.right {
    border-radius: ${ARROW_SIZE}px 0px 0px ${ARROW_SIZE}px;
    padding-left: 4px;
    border-right: none;
  }
  &.left {
    border-radius: 0px ${ARROW_SIZE}px ${ARROW_SIZE}px 0px;
    padding-right: 4px;
    border-left: none;
  }
  &.scrollLeft {
    opacity: 1;
    pointer-events: all;
  }
  &.scrollRight {
    opacity: 1;
    pointer-events: all;
  }
`;

const StyledContent = styled('div')`
  position: relative;
`;

const StyledHeaderRow = styled('div')`
  position: sticky;
  background: ${({ theme }) => theme.palette.background.default};
  top: 0px;
  margin-bottom: -1px;
  display: grid;
  z-index: 1;
`;

const StyledHeaderCell = styled('div')`
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider};
  box-sizing: border-box;
  display: flex;
  flex-grow: 0;
  overflow: hidden;
  align-items: center;
  &.keyCell {
    padding-left: 13px;
  }
  &.trashedCell {
    padding: 8px 12px;
    border-left: 1px solid ${({ theme }) => theme.palette.divider};
  }
`;

const StyledCounterContainer = styled('div')`
  position: fixed;
  bottom: 0px;
  right: 0px;
  z-index: ${({ theme }) => theme.zIndex.drawer};
  display: flex;
  background: ${({ theme }) => theme.palette.background.paper};
  align-items: stretch;
  transition: opacity 0.3s ease-in-out;
  border-radius: 6px;
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  margin: ${({ theme }) => theme.spacing(2, 3, 2, 0)};
  white-space: nowrap;
  pointer-events: all;

  &.hidden {
    opacity: 0;
    pointer-events: none;
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

const StyledStretcher = styled('div')`
  font-family: monospace;
  height: 0px;
  overflow: hidden;
`;

const StyledIconButton = styled(IconButton)`
  flex-shrink: 0;
  width: 40px;
  height: 40px;
`;

type Props = {
  trashedKeys: TrashedKeyModel[];
  selectedKeys: number[];
  onToggleKey: (keyId: number) => void;
  onRestore: () => void;
  onDelete: () => void;
  canRestore: boolean;
  canDelete: boolean;
  languages: LanguageModel[];
  finalColumnSizes: string[];
  columnSizes: number[];
  startResize: (index: number) => void;
  resizeColumn: (index: number, size: number) => void;
  addResizer: (index: number, callback: () => void) => void;
  onFilterNamespace: (ns: string) => void;
  totalCount: number;
  isFetchingNextPage: boolean;
  hasNextPage: boolean;
  fetchNextPage: () => void;
  containerWidth: number;
};

export const TrashTable: React.FC<Props> = ({
  trashedKeys,
  selectedKeys,
  onToggleKey,
  onRestore,
  onDelete,
  canRestore,
  canDelete,
  languages,
  finalColumnSizes,
  columnSizes,
  startResize,
  resizeColumn,
  addResizer,
  onFilterNamespace,
  totalCount,
  isFetchingNextPage,
  hasNextPage,
  fetchNextPage,
  containerWidth,
}) => {
  const { t } = useTranslate();
  const tableRef = useRef<HTMLDivElement>(null);
  const verticalScrollRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);
  const [scrollIndex, setScrollIndex] = useState(1);
  const [toolbarVisible, setToolbarVisible] = useState(false);

  const fullWidth = columnSizes.reduce((a, b) => a + b, 0);

  const [scrollLeft, scrollRight] = useScrollStatus(verticalScrollRef, [
    fullWidth,
    containerWidth,
  ]);

  const [tablePosition, setTablePosition] = useState({ left: 0, right: 0 });

  useEffect(() => {
    const position = tableRef.current?.getBoundingClientRect();
    if (position) {
      const left = position.left;
      const right = document.body.offsetWidth - position.right;
      setTablePosition({ left, right });
    }
  }, [tableRef.current, containerWidth]);

  const hasMinimalHeight = useMediaQuery('(min-height: 400px)');

  function handleHorizontalScroll(direction: 'left' | 'right') {
    const element = verticalScrollRef.current;
    if (element) {
      const position = element.scrollLeft;
      element.scrollTo({
        left: position + (direction === 'left' ? -350 : +350),
      });
    }
  }

  const getVisibleRange = reactListRef.current?.getVisibleRange.bind(
    reactListRef.current
  );

  // Track scroll position for counter using ReactList's getVisibleRange
  const onScroll = useDebouncedCallback(
    () => {
      const [start, end] = getVisibleRange?.() || [0, 0];
      const fromBeginning = start;
      const toEnd = totalCount - 1 - end;
      const total = fromBeginning + toEnd || 1;
      const progress = (total - toEnd) / total;
      const newIndex = Math.round(progress * (totalCount - 1) + 1);
      setScrollIndex(newIndex);
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

  const handleFetchMore = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const handleScrollUp = useCallback(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  const counterContent = `${scrollIndex} / ${totalCount}`;

  return (
    <StyledTableContainer
      data-cy="trash-table"
      className={clsx({ scrollLeft, scrollRight })}
      ref={tableRef}
    >
      {hasMinimalHeight && (
        <Portal>
          <StyledScrollArrow
            className={clsx('right', { scrollRight })}
            style={{ right: tablePosition.right }}
            onClick={() => handleHorizontalScroll('right')}
          >
            <ChevronRight width={20} height={20} />
          </StyledScrollArrow>
          <StyledScrollArrow
            className={clsx('left', { scrollLeft })}
            style={{ left: tablePosition.left }}
            onClick={() => handleHorizontalScroll('left')}
          >
            <ChevronLeft width={20} height={20} />
          </StyledScrollArrow>
        </Portal>
      )}
      <StyledVerticalScroll ref={verticalScrollRef}>
        <StyledContent>
          <StyledHeaderRow
            style={{
              gridTemplateColumns: finalColumnSizes.join(' '),
              width: `calc(${finalColumnSizes.join(' + ')})`,
              height:
                HEADER_HEIGHT +
                (trashedKeys[0]?.namespace ? NAMESPACE_BANNER_SPACING : 0),
            }}
          >
            <StyledHeaderCell className="keyCell">
              <T keyName="trash_header_key" />
            </StyledHeaderCell>
            <StyledHeaderCell className="trashedCell">
              <T keyName="trash_header_trashed" />
            </StyledHeaderCell>
            {languages.map((language, i) => (
              <StyledHeaderCell key={language.tag}>
                <CellLanguage
                  language={language}
                  onResize={() => startResize(i)}
                />
              </StyledHeaderCell>
            ))}
          </StyledHeaderRow>

          {columnSizes.slice(0, -1).map((w, i) => {
            const left =
              columnSizes.slice(0, i + 1).reduce((a, b) => a + b, 0) +
              (i >= 0 ? TRASHED_COLUMN_WIDTH : 0);
            return (
              <ColumnResizer
                key={i}
                size={w}
                left={left}
                onResize={(size) => resizeColumn(i, size)}
                passResizeCallback={(callback) => addResizer(i, callback)}
              />
            );
          })}

          <ReactList
            ref={reactListRef}
            threshold={800}
            type="variable"
            itemSizeEstimator={(index, cache) => {
              return cache[index] || 84;
            }}
            // @ts-ignore
            scrollParentGetter={() => window}
            length={trashedKeys.length}
            useTranslate3d
            itemRenderer={(index) => {
              const key = trashedKeys[index];
              const isLast = index === trashedKeys.length - 1;
              if (isLast && !isFetchingNextPage && hasNextPage) {
                handleFetchMore();
              }

              const prevKey = index > 0 ? trashedKeys[index - 1] : null;
              const showNamespace =
                key.namespace &&
                (index === 0 || prevKey?.namespace !== key.namespace);

              return (
                <TrashRow
                  key={key.id}
                  data={key}
                  selected={selectedKeys.includes(key.id)}
                  onToggle={() => onToggleKey(key.id)}
                  onRestore={onRestore}
                  onDelete={onDelete}
                  canRestore={canRestore}
                  canDelete={canDelete}
                  languages={languages}
                  columnSizes={finalColumnSizes}
                  showNamespace={!!showNamespace}
                  onFilterNamespace={onFilterNamespace}
                />
              );
            }}
          />
        </StyledContent>
      </StyledVerticalScroll>

      <Portal>
        <StyledCounterContainer className={clsx({ hidden: !toolbarVisible })}>
          <StyledIndex>
            <span data-cy="trash-toolbar-counter">{counterContent}</span>
            <StyledStretcher>{counterContent}</StyledStretcher>
          </StyledIndex>
          <StyledDivider />
          <Tooltip title={t('translations_toolbar_to_top')} disableInteractive>
            <StyledIconButton
              data-cy="trash-toolbar-to-top"
              onClick={handleScrollUp}
              size="small"
              aria-label={t('translations_toolbar_to_top')}
            >
              <ChevronUp width={20} height={20} />
            </StyledIconButton>
          </Tooltip>
        </StyledCounterContainer>
      </Portal>
    </StyledTableContainer>
  );
};
