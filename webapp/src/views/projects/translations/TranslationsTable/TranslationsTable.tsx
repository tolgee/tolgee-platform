import { useCallback, useEffect, useMemo, useRef } from 'react';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';

import {
  useTranslationsSelector,
  useTranslationsActions,
} from '../context/TranslationsContext';
import { ColumnResizer } from '../ColumnResizer';
import { CellLanguage } from './CellLanguage';
import { RowTable } from './RowTable';
import { NamespaceBanner } from '../Namespace/NamespaceBanner';
import { useNsBanners } from '../context/useNsBanners';
import { NAMESPACE_BANNER_SPACING } from '../cell/styles';
import { ReactList } from 'tg.component/reactList/ReactList';
import clsx from 'clsx';
import { useScrollStatus } from './useScrollStatus';
import { useColumns } from '../useColumns';

const StyledContainer = styled('div')`
  position: relative;
  display: grid;
  margin: 10px 0px 0px -0px;
  border-left: 0px;
  border-right: 0px;
  background: ${({ theme }) => theme.palette.background.default};
  flex-grow: 1;

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
  overflow-x: scroll;
  overflow-y: hidden;
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
`;

const StyledHeaderCell = styled('div')`
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  box-sizing: border-box;
  display: flex;
  flex-grow: 0;
  overflow: hidden;
  &.keyCell {
    padding-left: 13px;
    padding-top: 8px;
  }
`;

type Props = {
  toolsPanelOpen: boolean;
};

export const TranslationsTable = ({ toolsPanelOpen }: Props) => {
  const tableRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);
  const verticalScrollRef = useRef<HTMLDivElement>(null);

  const { fetchMore, registerList, unregisterList } = useTranslationsActions();
  const translations = useTranslationsSelector((v) => v.translations);
  const translationsLanguages =
    useTranslationsSelector((v) => v.translationsLanguages) || [];

  const languages = useTranslationsSelector((v) => v.languages);
  const isFetchingMore = useTranslationsSelector((v) => v.isFetchingMore);
  const hasMoreToFetch = useTranslationsSelector((v) => v.hasMoreToFetch);

  const languageCols = useMemo(() => {
    if (languages && translationsLanguages) {
      return (
        translationsLanguages?.map((lang) => {
          return languages.find((l) => l.tag === lang)!;
        }, [] as any[]) || []
      ).filter(Boolean);
    } else {
      return [];
    }
  }, [translationsLanguages, languages]);

  const columns = useMemo(
    () => [null, ...translationsLanguages.map((tag) => tag)],
    [translationsLanguages]
  );

  const {
    columnSizes,
    columnSizesPercent,
    startResize,
    resizeColumn,
    addResizer,
  } = useColumns({
    tableRef,
    initialRatios: columns.map(() => 1),
    minSize: 300,
    deps: [toolsPanelOpen],
  });

  const handleFetchMore = useCallback(() => {
    fetchMore();
  }, [translations]);

  useEffect(() => {
    if (reactListRef.current) {
      registerList(reactListRef.current);
      return () => {
        unregisterList(reactListRef.current!);
      };
    }
  }, [reactListRef.current]);

  const nsBanners = useNsBanners();
  const isBannerOnFirstRow = nsBanners[0]?.row === 0;

  if (!translations) {
    return null;
  }

  const fullWidth = columnSizes.reduce((a, b) => a + b, 0);

  const [scrollLeft, scrollRight] = useScrollStatus(verticalScrollRef, [
    fullWidth,
  ]);

  return (
    <StyledContainer
      data-cy="translations-view-table"
      className={clsx({ scrollLeft, scrollRight })}
      ref={tableRef}
    >
      <StyledVerticalScroll ref={verticalScrollRef}>
        <StyledContent>
          <StyledHeaderRow
            data-cy="this-is-the-element"
            style={{
              gridTemplateColumns: columnSizesPercent.join(' '),
            }}
          >
            {columns.map((tag, i) => {
              const language = languages?.find((lang) => lang.tag === tag);
              return tag ? (
                language && (
                  <StyledHeaderCell key={i}>
                    <CellLanguage
                      colIndex={i - 1}
                      onResize={startResize}
                      language={language}
                    />
                  </StyledHeaderCell>
                )
              ) : (
                <StyledHeaderCell
                  key={i}
                  style={{
                    width: columnSizesPercent[i],
                    height:
                      39 + (isBannerOnFirstRow ? NAMESPACE_BANNER_SPACING : 0),
                  }}
                  className="keyCell"
                >
                  <T keyName="translation_grid_key_text" />
                </StyledHeaderCell>
              );
            })}
          </StyledHeaderRow>
          {columnSizes.slice(0, -1).map((w, i) => {
            const left = columnSizes.slice(0, i + 1).reduce((a, b) => a + b, 0);
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
            length={translations.length}
            useTranslate3d
            itemRenderer={(index) => {
              const row = translations[index];
              const isLast = index === translations.length - 1;
              if (isLast && !isFetchingMore && hasMoreToFetch) {
                handleFetchMore();
              }

              const nsBannerAfter = nsBanners.find((b) => b.row === index + 1);
              const nsBanner = nsBanners.find((b) => b.row === index);
              return (
                <div key={`${row.keyNamespace}.${row.keyId}`}>
                  {nsBanner && (
                    <NamespaceBanner
                      namespace={nsBanner}
                      maxWidth={columnSizes[0]}
                    />
                  )}
                  <RowTable
                    bannerBefore={Boolean(nsBanner)}
                    bannerAfter={Boolean(nsBannerAfter)}
                    data={row}
                    languages={languageCols}
                    columnSizes={columnSizesPercent}
                    onResize={startResize}
                  />
                </div>
              );
            }}
          />
        </StyledContent>
      </StyledVerticalScroll>
    </StyledContainer>
  );
};
