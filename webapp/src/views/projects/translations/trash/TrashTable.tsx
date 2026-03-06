import React, { useCallback, useRef } from 'react';
import { Portal, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { ReactList } from 'tg.component/reactList/ReactList';
import { CellLanguage } from '../TranslationsTable/CellLanguage';
import { ColumnResizer } from '../ColumnResizer';
import { TrashRow } from './TrashRow';
import { TrashedKeyModel } from './TrashRow';
import { components } from 'tg.service/apiSchema.generated';
import { useHorizontalScroll } from '../useHorizontalScroll';
import { ScrollableTableContainer } from '../ScrollableTableContainer';
import { useScrollCounter } from '../useScrollCounter';
import { FloatingScrollCounter } from '../FloatingScrollCounter';

type LanguageModel = components['schemas']['LanguageModel'];

const HEADER_HEIGHT = 39;
const NAMESPACE_BANNER_SPACING = 14;
const TRASHED_COLUMN_WIDTH = 200;

const StyledFixedCounter = styled('div')`
  position: fixed;
  bottom: 0px;
  right: 0px;
  z-index: ${({ theme }) => theme.zIndex.drawer};
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
  const reactListRef = useRef<ReactList>(null);

  const horizontalScroll = useHorizontalScroll(columnSizes, containerWidth);
  const { scrollIndex, toolbarVisible, handleScrollUp } = useScrollCounter(
    reactListRef,
    totalCount
  );

  const handleFetchMore = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  return (
    <>
      <ScrollableTableContainer
        data-cy="trash-table"
        className="trash-table-container"
        {...horizontalScroll}
      >
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
      </ScrollableTableContainer>

      <Portal>
        <StyledFixedCounter>
          <FloatingScrollCounter
            scrollIndex={scrollIndex}
            totalCount={totalCount}
            visible={toolbarVisible}
            onScrollUp={handleScrollUp}
            dataCyPrefix="trash"
          />
        </StyledFixedCounter>
      </Portal>
    </>
  );
};
