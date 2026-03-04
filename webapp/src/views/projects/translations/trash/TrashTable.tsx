import React, { useRef, useState, useEffect } from 'react';
import { Pagination, Portal, styled, useMediaQuery } from '@mui/material';
import { ChevronLeft, ChevronRight } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import clsx from 'clsx';
import { useScrollStatus } from 'tg.component/common/useScrollStatus';
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

const StyledPagination = styled('div')`
  display: flex;
  justify-content: center;
  padding: 16px 0;
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
  addResizer: (index: number, callback: any) => void;
  onFilterNamespace: (ns: string) => void;
  totalPages: number;
  page: number;
  onPageChange: (event: unknown, newPage: number) => void;
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
  totalPages,
  page,
  onPageChange,
  containerWidth,
}) => {
  const tableRef = useRef<HTMLDivElement>(null);
  const verticalScrollRef = useRef<HTMLDivElement>(null);

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

  function handleScroll(direction: 'left' | 'right') {
    const element = verticalScrollRef.current;
    if (element) {
      const position = element.scrollLeft;
      element.scrollTo({
        left: position + (direction === 'left' ? -350 : +350),
      });
    }
  }

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
            onClick={() => handleScroll('right')}
          >
            <ChevronRight width={20} height={20} />
          </StyledScrollArrow>
          <StyledScrollArrow
            className={clsx('left', { scrollLeft })}
            style={{ left: tablePosition.left }}
            onClick={() => handleScroll('left')}
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

          {trashedKeys.map((key, index) => {
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
          })}
        </StyledContent>
      </StyledVerticalScroll>

      {totalPages > 1 && (
        <StyledPagination>
          <Pagination
            count={totalPages}
            page={page + 1}
            onChange={onPageChange}
            color="primary"
          />
        </StyledPagination>
      )}
    </StyledTableContainer>
  );
};
