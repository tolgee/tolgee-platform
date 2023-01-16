import { useCallback, useMemo, useEffect, useRef } from 'react';
import ReactList from 'react-list';
import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import {
  useTranslationsSelector,
  useTranslationsActions,
} from '../context/TranslationsContext';
import { ColumnResizer } from '../ColumnResizer';
import { RowList } from './RowList';
import { TranslationsToolbar } from '../TranslationsToolbar';
import { NamespaceBanner } from '../Namespace/NamespaceBanner';
import { useNsBanners } from '../context/useNsBanners';
import {
  useColumnsActions,
  useColumnsContext,
} from '../context/ColumnsContext';
import { NAMESPACE_BANNER_SPACING } from '../cell/styles';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: flex;
  position: relative;
  margin: 10px 0px 100px 0px;
  border-left: 0px;
  border-right: 0px;
  background: ${({ theme }) => theme.palette.background.default};
  flex-grow: 1;
  flex-direction: column;
  align-items: stretch;
`;

export const TranslationsList = () => {
  const tableRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);
  const { fetchMore, registerList, unregisterList } = useTranslationsActions();
  const translations = useTranslationsSelector((v) => v.translations);
  const languages = useTranslationsSelector((v) => v.languages);
  const translationsLanguages = useTranslationsSelector(
    (v) => v.translationsLanguages
  );
  const isFetchingMore = useTranslationsSelector((v) => v.isFetchingMore);
  const hasMoreToFetch = useTranslationsSelector((v) => v.hasMoreToFetch);
  const cursorKeyId = useTranslationsSelector((c) => c.cursor?.keyId);

  const columnSizes = useColumnsContext((c) => c.columnSizes);
  const columnSizesPercent = useColumnsContext((c) => c.columnSizesPercent);
  const totalWidth = useColumnsContext((c) => c.totalWidth);

  const { startResize, resizeColumn, addResizer, resetColumns } =
    useColumnsActions();

  useEffect(() => {
    resetColumns([1, 3], tableRef);
  }, [tableRef]);

  const handleFetchMore = useCallback(() => {
    fetchMore();
  }, [translations]);

  const languagesRow = useMemo(
    () =>
      (translationsLanguages
        ?.map((tag) => {
          return languages?.find((l) => l.tag === tag);
        })
        .filter(Boolean) as LanguageModel[]) || [],
    [languages, translationsLanguages]
  );

  useEffect(() => {
    if (reactListRef.current) {
      registerList(reactListRef.current);
      return () => {
        unregisterList(reactListRef.current!);
      };
    }
  }, [reactListRef.current]);

  const nsBanners = useNsBanners();

  if (!translations) {
    return null;
  }

  return (
    <StyledContainer
      style={{ marginBottom: cursorKeyId ? 500 : undefined }}
      ref={tableRef}
      data-cy="translations-view-list"
    >
      {columnSizes.slice(0, -1).map((w, i) => {
        const left = columnSizes.slice(0, i + 1).reduce((a, b) => a + b, 0);
        return (
          <ColumnResizer
            passResizeCallback={(callback) => addResizer(i, callback)}
            key={i}
            size={w}
            left={left}
            onResize={(size) => resizeColumn(i, size)}
          />
        );
      })}
      <ReactList
        ref={reactListRef}
        useTranslate3d
        threshold={800}
        type="variable"
        itemSizeEstimator={(index, cache) => {
          return (
            cache[index] ||
            Math.max((translationsLanguages?.length || 0) * 68, 83) + 1
          );
        }}
        // @ts-ignore
        scrollParentGetter={() => window}
        length={translations.length}
        itemRenderer={(index) => {
          const row = translations[index];
          const isLast = index === translations.length - 1;
          if (isLast && !isFetchingMore && hasMoreToFetch) {
            handleFetchMore();
          }
          const nsBannerAfter = nsBanners.find((b) => b.row === index + 1);
          const nsBanner = nsBanners.find((b) => b.row === index);
          const bannerSpacing = nsBanner?.row === 0;

          return (
            <div
              key={row.keyId}
              style={{
                paddingTop: bannerSpacing
                  ? NAMESPACE_BANNER_SPACING
                  : undefined,
              }}
            >
              {nsBanner && (
                <NamespaceBanner
                  namespace={nsBanner}
                  maxWidth={columnSizes[0]}
                />
              )}
              <RowList
                bannerBefore={Boolean(nsBanner)}
                bannerAfter={Boolean(nsBannerAfter)}
                data={row}
                languages={languagesRow}
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
