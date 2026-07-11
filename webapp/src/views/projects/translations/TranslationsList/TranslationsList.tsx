import { useCallback, useMemo, useEffect, useRef } from 'react';
import { styled } from '@mui/material';
import { ReactList } from 'tg.component/reactList/ReactList';

import { components } from 'tg.service/apiSchema.generated';
import {
  useTranslationsSelector,
  useTranslationsActions,
} from '../context/TranslationsContext';
import { ColumnResizer } from '../ColumnResizer';
import { RowList } from './RowList';
import { NamespaceBanner } from '../Namespace/NamespaceBanner';
import { useNsBanners } from '../context/useNsBanners';

import { NAMESPACE_BANNER_SPACING } from '../cell/styles';
import { useColumns } from '../useColumns';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: flex;
  position: relative;
  border-left: 0px;
  border-right: 0px;
  background: ${({ theme }) => theme.palette.background.default};
  flex-grow: 1;
  flex-direction: column;
  align-items: stretch;
  z-index: 4;
`;

type Props = {
  width: number;
};

export const TranslationsList = ({ width }: Props) => {
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

  const {
    columnSizes,
    columnSizesPercent,
    startResize,
    resizeColumn,
    addResizer,
  } = useColumns({
    width,
    initialRatios: [1, 3],
  });

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
    <StyledContainer ref={tableRef} data-cy="translations-view-list">
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
              key={`${row.keyNamespace}.${row.keyId}`}
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
                columnSizesPercent={columnSizesPercent}
                columnSizes={columnSizes}
                onResize={startResize}
              />
            </div>
          );
        }}
      />
    </StyledContainer>
  );
};
