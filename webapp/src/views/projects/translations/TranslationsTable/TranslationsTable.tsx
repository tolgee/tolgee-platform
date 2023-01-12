import { useCallback, useEffect, useMemo, useRef } from 'react';
import ReactList from 'react-list';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';

import {
  useTranslationsSelector,
  useTranslationsActions,
} from '../context/TranslationsContext';
import { ColumnResizer } from '../ColumnResizer';
import { CellLanguage } from './CellLanguage';
import { RowTable } from './RowTable';
import { TranslationsToolbar } from '../TranslationsToolbar';
import { NamespaceBanner } from '../Namespace/NamespaceBanner';
import { useNsBanners } from '../context/useNsBanners';
import {
  useColumnsActions,
  useColumnsContext,
} from '../context/ColumnsContext';
import { NAMESPACE_BANNER_SPACING } from '../cell/styles';

const StyledContainer = styled('div')`
  position: relative;
  margin: 10px 0px 100px 0px;
  border-left: 0px;
  border-right: 0px;
  background: ${({ theme }) => theme.palette.background.default};
  flex-grow: 1;
`;

const StyledHeaderRow = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
  border-width: 1px 0px 1px 0px;
  position: sticky;
  background: ${({ theme }) => theme.palette.background.default};
  top: 0px;
  margin-bottom: -1px;
  display: flex;
`;

const StyledHeaderCell = styled('div')`
  box-sizing: border-box;
  display: flex;
  flex-grow: 0;
  overflow: hidden;
  &.keyCell {
    padding-left: 13px;
    padding-top: 8px;
  }
`;

export const TranslationsTable = () => {
  const tableRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);

  const { fetchMore, registerList, unregisterList } = useTranslationsActions();
  const translations = useTranslationsSelector((v) => v.translations);
  const translationsLanguages =
    useTranslationsSelector((v) => v.translationsLanguages) || [];

  const languages = useTranslationsSelector((v) => v.languages);
  const isFetchingMore = useTranslationsSelector((v) => v.isFetchingMore);
  const hasMoreToFetch = useTranslationsSelector((v) => v.hasMoreToFetch);
  const cursorKeyId = useTranslationsSelector((c) => c.cursor?.keyId);

  const columnSizes = useColumnsContext((c) => c.columnSizes);
  const columnSizesPercent = useColumnsContext((c) => c.columnSizesPercent);
  const totalWidth = useColumnsContext((c) => c.totalWidth);

  const { startResize, resizeColumn, addResizer, resetColumns } =
    useColumnsActions();

  const languageCols = useMemo(() => {
    if (languages && translationsLanguages) {
      return (
        translationsLanguages?.map((lang) => {
          return languages.find((l) => l.tag === lang)!;
        }, [] as any[]) || []
      );
    } else {
      return [];
    }
  }, [translationsLanguages, languages]);

  const columns = useMemo(
    () => [null, ...translationsLanguages.map((tag) => tag)],
    [translationsLanguages]
  );

  useEffect(() => {
    resetColumns(
      columns.map(() => 1),
      tableRef
    );
  }, [languageCols, tableRef]);

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

  return (
    <StyledContainer
      style={{ marginBottom: cursorKeyId ? 500 : undefined }}
      ref={tableRef}
      data-cy="translations-view-table"
    >
      <StyledHeaderRow>
        {columns.map((tag, i) => {
          const language = languages!.find((lang) => lang.tag === tag)!;
          return tag ? (
            <StyledHeaderCell key={i} style={{ width: columnSizesPercent[i] }}>
              <CellLanguage
                colIndex={i - 1}
                onResize={startResize}
                language={language}
              />
            </StyledHeaderCell>
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
              <T>translation_grid_key_text</T>
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
            <div key={row.keyId}>
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
      <TranslationsToolbar width={totalWidth} />
    </StyledContainer>
  );
};
